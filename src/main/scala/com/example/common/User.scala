package com.example.common

import com.example.UserRegistry.ActionPerformed
import com.sksamuel.elastic4s.analysis.{Analysis, CustomAnalyzer, EdgeNGramTokenizer}
import com.sksamuel.elastic4s.fields.{IntegerField, KeywordField, TextField}
import com.sksamuel.elastic4s.requests.indexes.CreateIndexRequest
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import com.sksamuel.elastic4s.{Hit, Indexable}
import com.typesafe.scalalogging.LazyLogging
import spray.json.{DefaultJsonProtocol, JsObject, JsValue, JsonFormat, enrichAny}

import scala.collection.immutable


final case class User(name: String, age: Int, countryOfResidence: String, id: String) {
}

final case class Users(users: immutable.Seq[User])

object User extends LazyLogging {

  import com.sksamuel.elastic4s.ElasticDsl._

  val index: CreateIndexRequest = createIndex("users").mapping(
    properties(
      IntegerField("age"),
      TextField("name") analyzer "analyzer" searchAnalyzer "standard" fields KeywordField("keyword"),
      KeywordField("country")
    )
  ).analysis(
    Analysis(
      List(CustomAnalyzer("analyzer", "engram", Nil, List("lowercase", "unique"))),
      List(EdgeNGramTokenizer("engram", 3, 7))
    )
  )

  import DefaultJsonProtocol._


  implicit val actionPerformedJsonFormat = jsonFormat1(ActionPerformed)

  implicit object UserIndexable extends Indexable[User] {
    override def json(t: User): String =
      s""" { "name" : "${t.name}", "age" : ${t.age}, "country" : "${t.countryOfResidence}" } """
  }

  implicit val userJsonFormat: JsonFormat[User] = new JsonFormat[User] {
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

  def fromHit(hit: Hit): Option[User] = {
    val fields = hit.sourceAsMap
    for {
      name <- fields.get("name").map(_.asInstanceOf[String])
      age <- fields.get("age").map(_.asInstanceOf[Int])
      country <- fields.get("country").map(_.asInstanceOf[String])
    } yield User(name, age, country, hit.id)
  }

  def fromResponse(response: SearchResponse): Users = {
    Users(response.hits.hits.flatMap(fromHit))
  }

  implicit val usersJsonFormat: JsonFormat[Users] = jsonFormat1(Users)
}

