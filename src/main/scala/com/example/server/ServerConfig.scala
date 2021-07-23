package com.example.server

import com.typesafe.config.Config

import scala.util.Try

case class ServerConfig(host: String, port: String, esHost: String, esPort: String)

object ServerConfig {
  def apply(config: Config): ServerConfig = ServerConfig(
    Try(config.getString("server.host")) getOrElse "0.0.0.0",
    Try(config.getString("server.port")) getOrElse "8080",
    Try(config.getString("es.host")) getOrElse "0.0.0.0",
    Try(config.getString("es.port")) getOrElse "9200"
  )
}
