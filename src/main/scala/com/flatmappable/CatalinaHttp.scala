package com.flatmappable

import java.util.UUID

import com.typesafe.scalalogging.LazyLogging
import com.ubirch.protocol.Protocol

import scala.util.Try

object CatalinaHttp extends cask.MainRoutes with LazyLogging {

  case class BadRequestException(message: String) extends Exception(message)

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
      val res = DataSending.send(identity, pass, DataGenerator.toBase64(hash), DataGenerator.toHex(upp))

      if (res.status >= 200 && res.status < 300) {
        cask.Response(DataGenerator.toBase64(hash), res.status)
      } else {
        logger.error(s"Error Sending UPP=${DataGenerator.toBase64(hash)} Status=${res.status}")
        cask.Response(s"Error Sending UPP with Hash=${DataGenerator.toBase64(hash)}", res.status)
      }

    } catch {
      case BadRequestException(message) =>
        logger.error("Bad Request={}", message)
        cask.Response(message, 400)
      case e: Exception =>
        logger.error("Internal error", e)
        cask.Response("Internal Error", 500)
    }
  }

  initialize()
}
