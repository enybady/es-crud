package com.example.server

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.example.common.{User, Users}
import com.example.server.UserRegistry.{ActionPerformed, CreateIndex, CreateUser, DeleteUser, FindUsers, GetUser, GetUserResponse, GetUsers, UpdateUser}

import scala.concurrent.Future
class UserRoutes(userRegistry: ActorRef[UserRegistry.Command])(implicit val system: ActorSystem[_]) {

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import com.example.common.JsonFormats._

  private implicit val timeout = Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))

  def start(): Future[ActionPerformed] =
    userRegistry.ask(CreateIndex)
  def getUsers(): Future[Users] =
    userRegistry.ask(GetUsers)
  def findUser(name: String): Future[Users] =
    userRegistry.ask(FindUsers(name, _))
  def getUser(name: String): Future[GetUserResponse] =
    userRegistry.ask(GetUser(name, _))
  def createUser(user: User): Future[ActionPerformed] =
    userRegistry.ask(CreateUser(user, _))
  def updateUser(user: User): Future[ActionPerformed] =
    userRegistry.ask(UpdateUser(user, _))
  def deleteUser(name: String): Future[ActionPerformed] =
    userRegistry.ask(DeleteUser(name, _))

  val userRoutes: Route =
    pathPrefix("users") {
      concat(
        pathEnd {
          concat(
            get {
              complete(getUsers())
            },
            post {
              entity(as[User]) { user =>
                onSuccess(createUser(user)) { performed =>
                  complete((StatusCodes.Created, performed))
                }
              }
            },
            put {
              entity(as[User]) { user =>
                onSuccess(createUser(user)) { performed =>
                  complete((StatusCodes.Created, performed))
                }
              }
            }
          )
        },
        (path("start") & pathEnd & get) {
          onSuccess(start()) { performed =>
            complete((StatusCodes.OK, performed))
          }
        },
        (path("find" / Segment) & pathEnd & get) { name =>
          onSuccess(findUser(name)) { users =>
            complete(users)
          }
        },
        path(Segment) { id =>
          concat(
            get {
              rejectEmptyResponse {
                onSuccess(getUser(id)) { response =>
                  complete(response.maybeUser)
                }
              }
            },
            delete {
              onSuccess(deleteUser(id)) { performed =>
                complete((StatusCodes.OK, performed))
              }
            },
            put {
              entity(as[User]) { user =>
                onSuccess(createUser(user.copy(id = id))) { performed =>
                  complete((StatusCodes.Created, performed))
                }
              }
            }
          )
        })
    }
}
