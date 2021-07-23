package com.example.client

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.RouteResult.Complete
import akka.util.Timeout

import scala.concurrent.ExecutionContext.Implicits.global

class UserRoutes()(implicit val system: ActorSystem[_]) {

  private implicit val timeout = Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))

  val userRoutes: Route = ctx =>
    Http().singleRequest(ctx.request.withUri("http://0.0.0.0:8080")).map(Complete)
}
