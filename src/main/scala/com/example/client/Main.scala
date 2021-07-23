package com.example.client

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import com.newmotion.akka.rabbitmq.{Channel, ConnectionActor, ConnectionFactory}
import com.sksamuel.elastic4s.http.JavaClient
import com.sksamuel.elastic4s.{ElasticClient, ElasticNodeEndpoint, ElasticProperties}
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContext.Implicits._
import scala.util.{Failure, Success}

//#main-class
object Main extends LazyLogging {
  //#start-http-server
  private def startHttpServer(routes: Route)(implicit system: ActorSystem): Unit = {
    val futureBinding = Http().newServerAt("0.0.0.0", 8081).bind(routes)
    futureBinding.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info("Server online at http://{}:{}/", address.getHostString, address.getPort)
      case Failure(ex) =>
        system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
        system.terminate()
    }
  }


  //#start-http-server
  def main(args: Array[String]): Unit = {
    //#server-bootstrapping
    val rootBehavior = Behaviors.setup[Nothing] { context =>
      val routes = new UserRoutes()(context.system)
      startHttpServer(routes.userRoutes)(context.system)

      context.log.info(s"Creating index for users")
      val factory = new ConnectionFactory()
      val conn = context.spawn(ConnectionActor.props())

      Behaviors.empty
    }
    val system = ActorSystem("main")


    //#server-bootstrapping
  }
}
//#main-class
