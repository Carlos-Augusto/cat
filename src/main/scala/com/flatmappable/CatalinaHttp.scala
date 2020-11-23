package com.flatmappable

import java.nio.charset.StandardCharsets
import java.util.UUID

import cask.model.Response
import com.flatmappable.util.{ Configs, JsonHelper, ResponseData }
import com.typesafe.scalalogging.LazyLogging
import com.ubirch.protocol.Protocol
import ujson.Obj

import scala.util.Try

abstract class CatalinaHttpBase extends cask.MainRoutes with LazyLogging {

  init(http = true)

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
  def hello(): String = {
    """
      |
      |
      |   |\      _,,,---,,_
      |   /,`.-'`'    -.  ;-;;,_
      |  |,4-  ) )-,_..;\ (  `'-'
      | '---''(_/--'  `-'\_)  Felix Lee <flee@cse.psu.edu>
      |
      |------------------------------------------------
      |Thank you for visiting https://asciiart.website/
      |This ASCII pic can be found at
      |https://asciiart.website/index.php?art=animals/cats
      |""".stripMargin
  }

  private lazy val _configs: String =
    s"""
     |- VERSION: $version
     |- ENV: ${Configs.ENV}
     |- PORT: ${Configs.CAT_HTTP_PORT}
     |- HOST: $host
     |- DATA_SENDING_URL:${Configs.DATA_SENDING_URL}
     |""".stripMargin

  @cask.get("/configs")
  def configs(): String = _configs

  protected def sendBody(contentType: Option[String], headers: Map[String, scala.collection.Seq[String]], body: Array[Byte]): (String, Array[Byte]) = {
    contentType.collect {
      case "application/json" =>
        headers
          .get("x-json-format")
          .flatMap(_.headOption)
          .collect {
            case "compact" => JsonHelper.compact(body)
            case "pretty" => JsonHelper.pretty(body)
            case "none" => JsonHelper.compact(body)
              .map(_ => (new String(body, StandardCharsets.UTF_8), body))
          }
          .getOrElse(JsonHelper.compact(body))
    }
      .getOrElse(Right("", body))
      .getOrElse(throw BadRequestException("Body is malformed"))
  }

  protected def sendData(uuid: UUID, password: String, hash: Array[Byte], upp: Array[Byte], extraHeaders: Map[String, Seq[String]]): ResponseData[Array[Byte]] = {
    DataSending.send(uuid, password, toBase64AsString(hash), toHex(upp), extraHeaders)
  }

  @cask.post("/send/:uuid")
  def send(uuid: String, request: cask.Request): Response[Obj] = {
    try {
      val identity = Try(UUID.fromString(uuid)).getOrElse(throw BadRequestException("Invalid uuid"))
      val body = request.bytes
      if (body.isEmpty) throw BadRequestException("Empty body")
      val privateKey = request.headers.get("x-pk").flatMap(_.headOption).getOrElse(throw BadRequestException("No x-pk"))
      val pass = request.headers.get("x-pass").flatMap(_.headOption).getOrElse(throw BadRequestException("No x-pass"))

      val headersToRedirect = request.headers
        .filter { case (k, v) => k.startsWith("x-proxy-") && v.forall(_.nonEmpty) }
        .map { case (k, v) => (k.replaceFirst("x-proxy-", "").trim, v.toSeq.map(_.trim)) }
        .filter { case (k, v) => k.nonEmpty && v.forall(_.nonEmpty) }
        .filterNot { case (k, _) => k.endsWith("-") || k.startsWith("-") }

      val contentType = request.headers
        .filter { case (k, _) => k.startsWith("content-type") }
        .flatMap { case (_, v) => v }
        .headOption

      val (asString, asBytes) = sendBody(contentType, request.headers, body)

      if (asString.nonEmpty) {
        logger.info("body={}", asString)
      }

      val (_, upp, hash) = DataGenerator.single(identity, asBytes, privateKey, Protocol.Format.MSGPACK)
      val res = sendData(identity, pass, hash, upp, headersToRedirect)

      if (res.status >= OK && res.status < MULTIPLE_CHOICE) {
        cask.Response(ResponseMessage(res.status, "Success", toBase64AsString(hash)).toJson, res.status)
      } else if (res.status == KNOWN_UPP) {
        logger.error(s"UPP already known=${toBase64AsString(hash)} Status=${res.status}")
        cask.Response(ResponseMessage(res.status, "KnownUPPError", toBase64AsString(hash)).toJson, res.status)
      } else if (res.status == UNAUTHORIZED) {
        logger.error(s"UPP was rejected=${toBase64AsString(hash)} Status=${res.status}")
        cask.Response(ResponseMessage(res.status, "Unauthorized", toBase64AsString(hash)).toJson, res.status)
      } else {
        logger.error(s"Error Sending UPP=${toBase64AsString(hash)} Status=${res.status}")
        cask.Response(ResponseMessage(res.status, "SendingUPPError", s"Error Sending UPP with Hash=${toBase64AsString(hash)}").toJson, res.status)
      }

    } catch {
      case BadRequestException(message) =>
        logger.error("Bad Request={}", message)
        cask.Response(ResponseMessage(BAD_REQUEST, "BadRequest", message).toJson, BAD_REQUEST)
      case e: Exception =>
        logger.error("Internal error", e)
        cask.Response(ResponseMessage(INTERNAL_SERVER_ERROR, "InternalError").toJson, INTERNAL_SERVER_ERROR)
    }
  }

  override def port: Int = Configs.CAT_HTTP_PORT

  override def host: String = "0.0.0.0"

  initialize()

}

object CatalinaHttp extends CatalinaHttpBase
