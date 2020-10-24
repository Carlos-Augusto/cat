package com.flatmappable

import java.util.UUID

import com.typesafe.scalalogging.LazyLogging
import com.ubirch.protocol.Protocol
import ujson.Obj

import scala.util.Try

object CatalinaHttp extends cask.MainRoutes with LazyLogging {

  init()

  case class BadRequestException(message: String) extends Exception(message)

  case class ResponseMessage(status: Int, message: String, data: String = "") {
    def toJson: Obj = {
      ujson.Obj(
        "status" -> status,
        "message" -> message,
        "data" -> data
      )
    }
  }

  @cask.get("/")
  def hello() = {
    "This is Catalina."
  }

  @cask.post("/send/:uuid")
  def send(uuid: String, request: cask.Request) = {
    try {
      val identity = Try(UUID.fromString(uuid)).getOrElse(throw BadRequestException("Invalid uuid"))
      val body = request.bytes
      if (body.isEmpty) throw BadRequestException("Empty body")
      val privateKey = request.headers.get("x-pk").flatMap(_.headOption).getOrElse(throw BadRequestException("No x-pk"))
      val pass = request.headers.get("x-pass").flatMap(_.headOption).getOrElse(throw BadRequestException("No x-pass"))

      val (_, upp, hash) = DataGenerator.single(identity, body, privateKey, Protocol.Format.MSGPACK)
      val res = DataSending.send(identity, pass, toBase64AsString(hash), toHex(upp))

      if (res.status >= 200 && res.status < 300) {
        cask.Response(ResponseMessage(res.status, "Success", toBase64AsString(hash)).toJson, res.status)
      } else if (res.status == 409) {
        logger.error(s"UPP already known=${toBase64AsString(hash)} Status=${res.status}")
        cask.Response(ResponseMessage(res.status, "KnownUPPError", toBase64AsString(hash)).toJson, res.status)
      } else {
        logger.error(s"Error Sending UPP=${toBase64AsString(hash)} Status=${res.status}")
        cask.Response(ResponseMessage(res.status, "SendingUPPError", s"Error Sending UPP with Hash=${toBase64AsString(hash)}").toJson, res.status)
      }

    } catch {
      case BadRequestException(message) =>
        logger.error("Bad Request={}", message)
        cask.Response(ResponseMessage(400, "BadRequest", message).toJson, 400)
      case e: Exception =>
        logger.error("Internal error", e)
        cask.Response(ResponseMessage(500, "InternalError").toJson, 500)
    }
  }

  override def port: Int = 8080

  override def host: String = "0.0.0.0"

  initialize()
}
