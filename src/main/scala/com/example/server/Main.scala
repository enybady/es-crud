package com.example.server

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import com.sksamuel.elastic4s.http.JavaClient
import com.sksamuel.elastic4s.{ElasticClient, ElasticNodeEndpoint, ElasticProperties}
import com.typesafe.scalalogging.LazyLogging

import scala.util.{Failure, Success}

object Main extends LazyLogging {
  private def startHttpServer(routes: Route, config: ServerConfig)(implicit system: ActorSystem[_]): Unit = {
    import system.executionContext

    val futureBinding = Http().newServerAt(config.host, config.port.toInt).bind(routes)
    futureBinding.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info("Server online at http://{}:{}/", address.getHostString, address.getPort)
      case Failure(ex) =>
        system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
        system.terminate()
    }
  }


  def main(args: Array[String]): Unit = {
    val rootBehavior = Behaviors.setup[Nothing] { context =>
      val config = ServerConfig(context.system.settings.config)
      val client = ElasticClient(JavaClient(ElasticProperties(s"http://${config.esHost}:${config.esPort}")))
      val userRegistryActor = context.spawn(new UserRegistry(client).apply(), "UserRegistryActor")
      context.watch(userRegistryActor)

      val routes = new UserRoutes(userRegistryActor)(context.system)
      startHttpServer(routes.userRoutes, config)(context.system)

      context.log.info(s"Creating index for users")

      Behaviors.empty
    }
    val system = ActorSystem[Nothing](rootBehavior, "HelloAkkaHttpServer")
  }


}
