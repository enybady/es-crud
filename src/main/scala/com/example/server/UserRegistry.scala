package com.example.server

//#user-registry-actor

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.example.common.{User, Users}
import com.example.server.UserRegistry.{ActionPerformed, _}
import com.sksamuel.elastic4s.ElasticApi.{deleteById, get, indexInto, matchAllQuery, search, updateById}
import com.sksamuel.elastic4s.ElasticDsl.{CreateIndexHandler, DeleteByIdHandler, GetHandler, IndexHandler, SearchHandler, UpdateHandler}
import com.sksamuel.elastic4s.requests.delete.DeleteResponse
import com.sksamuel.elastic4s.requests.get.GetResponse
import com.sksamuel.elastic4s.requests.indexes.{CreateIndexResponse, IndexResponse}
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import com.sksamuel.elastic4s.requests.update.UpdateResponse
import com.sksamuel.elastic4s.{ElasticClient, RequestFailure, RequestSuccess, Response}
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.control.NonFatal

object UserRegistry {

  sealed trait Command

  final case class GetUsers(replyTo: ActorRef[Users]) extends Command

  final case class FindUsers(name: String, replyTo: ActorRef[Users]) extends Command

  final case class CreateUser(user: User, replyTo: ActorRef[ActionPerformed]) extends Command

  final case class UpdateUser(user: User, replyTo: ActorRef[ActionPerformed]) extends Command

  final case class GetUser(id: String, replyTo: ActorRef[GetUserResponse]) extends Command

  final case class DeleteUser(name: String, replyTo: ActorRef[ActionPerformed]) extends Command

  final case class CreateIndex(replyTo: ActorRef[ActionPerformed]) extends Command

  final case class GetUserResponse(maybeUser: Option[User])

  final case class ActionPerformed(description: String)

}

class UserRegistry(esClient: ElasticClient) extends LazyLogging {
  val indexName = "users"

  private def prepareResponse[T](resp: Response[T]): Future[T] = resp match {
    case r: RequestSuccess[T] => Future.successful(r.result)
    case e: RequestFailure =>
      Future.failed(e.error.asException)
  }

  private def foldF[T, R](f: Future[Response[T]], default: R, op: T => R)(implicit ec: ExecutionContext): Future[R] = f
    .flatMap(prepareResponse)
    .map(op)
    .recover { case NonFatal(e) =>
      logger.error("Error: ", e)
      default
    }

  private def updateUser(user: User)(implicit ec: ExecutionContext) =
    foldF[UpdateResponse, ActionPerformed](
      esClient.execute(updateById("users", user.id).doc(user)),
      ActionPerformed(s"User ${user.id} is not updated."),
      _ => ActionPerformed(s"User ${user.id} updated.")
    )

  private def createUser(user: User)(implicit ec: ExecutionContext) =
    foldF[IndexResponse, ActionPerformed](
      esClient.execute(indexInto("users").doc(user)),
      ActionPerformed(s"User ${user.id} is not created."),
      _ => ActionPerformed(s"User ${user.id} created.")
    )

  def apply(): Behavior[Command] =
    Behaviors.receive { (context, message) =>
      implicit val ec: ExecutionContextExecutor = context.executionContext

      message match {
        case CreateIndex(replyTo) =>
          foldF[CreateIndexResponse, ActionPerformed](
            esClient.execute(User.index),
            ActionPerformed("index creation failed"),
            value => ActionPerformed(if (value.acknowledged) "index created" else "index creation failed")
          ) foreach replyTo.tell

        case FindUsers(name, replyTo) =>
          foldF[SearchResponse, Users](
            esClient.execute(search("users").query(name)),
            Users(Seq.empty),
            User.fromResponse
          ) foreach replyTo.tell

        case GetUsers(replyTo) =>
          foldF[SearchResponse, Users](
            esClient.execute(search("users").query(matchAllQuery)),
            Users(Seq.empty),
            User.fromResponse
          ) foreach replyTo.tell

        case GetUser(id, replyTo) =>
          foldF[GetResponse, GetUserResponse](
            esClient.execute(get("users", id)),
            GetUserResponse(None),
            value => GetUserResponse(User.fromHit(value))
          ) foreach replyTo.tell

        case CreateUser(user, replyTo) =>
          createUser(user).foreach(replyTo ! _)

        case UpdateUser(user, replyTo) =>
          (if (user.id.isEmpty) createUser(user) else updateUser(user)).foreach(replyTo ! _)

        case DeleteUser(id, replyTo) =>
          foldF[DeleteResponse, ActionPerformed](
            esClient.execute(deleteById("users", id)),
            ActionPerformed(s"User $id is not deleted."),
            value => ActionPerformed(value.result)
          ) foreach replyTo.tell
      }
      Behaviors.same
    }

}