package com.example.common

import com.example.server.UserRegistry.ActionPerformed
import spray.json.{DefaultJsonProtocol, JsObject, JsValue, RootJsonFormat, enrichAny}

object JsonFormats {
  // import the default encoders for primitive types (Int, String, Lists etc)

  import DefaultJsonProtocol._

  implicit val userJsonFormat: RootJsonFormat[User] = new RootJsonFormat[User] {
    override def read(json: JsValue): User = {
      val fields = json.asJsObject("Person object expected").fields
      User(
        name = fields("name").convertTo[String],
        age = fields("age").convertTo[Int],
        countryOfResidence = fields("countryOfResidence").convertTo[String],
        id = fields.get("id").fold("")(_.convertTo[String])
      )
    }

    override def write(user: User): JsValue = JsObject(
      "name" -> user.name.toJson,
      "age" -> user.age.toJson,
      "countryOfResidence" -> user.countryOfResidence.toJson,
      "id" -> user.id.toJson
    )
  }

  implicit val usersJsonFormat = jsonFormat1(Users)

  implicit val actionPerformedJsonFormat = jsonFormat1(ActionPerformed)
}
