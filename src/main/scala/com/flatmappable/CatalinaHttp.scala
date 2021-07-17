package com.flatmappable

import java.nio.charset.{ Charset, StandardCharsets }
import java.util.UUID

import cask.model.Response
import com.flatmappable.util.{ Configs, JsonHelper, Logging, ResponseData }
import com.ubirch.protocol.Protocol
import ujson.Obj

import scala.util.{ Failure, Success, Try }

abstract class CatalinaHttpBase extends cask.MainRoutes with Logging {

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
     |- ENV: ${Configs.ENV.name}
     |- PORT: ${Configs.CAT_HTTP_PORT}
     |- HOST: $host
     |- DATA_SENDING_URL:${Configs.DATA_SENDING_URL}
     |- RUNTIME:${Runtime.version().toString}
     |""".stripMargin

  @cask.get("/configs")
  def configs(): String = _configs

  protected def sendBody(contentType: Option[String], charset: Option[Charset], headers: Map[String, scala.collection.Seq[String]], body: Array[Byte]): (String, Array[Byte]) = {
    contentType.collect {
      case "application/json" =>
        headers
          .get("x-json-format")
          .flatMap(_.headOption)
          .collect {
            case "compact" => JsonHelper.compact(body)
            case "pretty" => JsonHelper.pretty(body)
            case "none" => JsonHelper.compact(body)
              .toTry
              .map(_ => (new String(body, StandardCharsets.UTF_8), body))
              .toEither
          }
          .getOrElse(JsonHelper.compact(body))
      case "text/plain" => Try((new String(body, charset.getOrElse(StandardCharsets.UTF_8)), body)).toEither
    }
      .getOrElse(Right(("", body)))
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

      val messageDigest = request.headers.get("x-msg-digest-algo")
        .flatMap(_.headOption).map(_.toLowerCase) match {
          case Some("sha256") => DataGenerator.SHA256
          case Some("sha512") => DataGenerator.SHA512
          case _ => DataGenerator.SHA512
        }

      val headersToRedirect = request.headers
        .filter { case (k, v) => k.startsWith("x-proxy-") && v.forall(_.nonEmpty) }
        .map { case (k, v) => (k.replaceFirst("x-proxy-", "").trim, v.toSeq.map(_.trim)) }
        .filter { case (k, v) => k.nonEmpty && v.forall(_.nonEmpty) }
        .filterNot { case (k, _) => k.endsWith("-") || k.startsWith("-") }

      val (contentType, charset) = getContentType(request.headers)

      val (asString, asBytes) = sendBody(contentType, charset, request.headers, body)

      if (asString.nonEmpty) {
        logger.info("body={}", asString)
      }

      DataGenerator.buildMessage(
        identity,
        asBytes,
        privateKey,
        Protocol.Format.MSGPACK,
        messageDigest,
        withNonce = false
      ) match {
          case Failure(exception) => throw exception
          case Success(data) =>

            val res = sendData(identity, pass, data.hash, data.upp, headersToRedirect)

            if (res.status >= OK && res.status < MULTIPLE_CHOICE) {
              cask.Response(ResponseMessage(res.status, "Success", data.hashAsBase64).toJson, res.status)
            } else if (res.status == KNOWN_UPP) {
              logger.error(s"UPP already known=${data.hashAsBase64} Status=${res.status}")
              cask.Response(ResponseMessage(res.status, "KnownUPPError", data.hashAsBase64).toJson, res.status)
            } else if (res.status == UNAUTHORIZED) {
              logger.error(s"UPP was rejected=${data.hashAsBase64} Status=${res.status}")
              cask.Response(ResponseMessage(res.status, "Unauthorized", data.hashAsBase64).toJson, res.status)
            } else {
              logger.error(s"Error Sending UPP=${data.hashAsBase64} Status=${res.status}")
              cask.Response(ResponseMessage(res.status, "SendingUPPError", s"Error Sending UPP with Hash=${data.hashAsBase64}").toJson, res.status)
            }

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

  def getContentType(headers: Map[String, scala.collection.Seq[String]]): (Option[String], Option[Charset]) = {
    val contentTypeAndCharset = headers
      .filter { case (k, _) => k.startsWith("content-type") }
      .flatMap { case (_, v) => v }
      .flatMap { v =>
        v.split(";", 2)
          .toList
          .map(_.replaceAll(" ", ""))
          .map(_.trim)
      }
      .toList

    contentTypeAndCharset match {
      case List(ct, cs) if cs.toLowerCase.startsWith("charset") =>

        (Some(ct), Try(cs.split("=", 2).toList)
          .map(_.reverse)
          .map(_.headOption).map {
            case Some("US-ASCII") => StandardCharsets.US_ASCII
            case Some("ISO-8859-1") => StandardCharsets.ISO_8859_1
            case Some("UTF-8") => StandardCharsets.UTF_8
            case Some("UTF-16BE") => StandardCharsets.UTF_16BE
            case Some("UTF-16LE") => StandardCharsets.UTF_16LE
            case Some("UTF-16") => StandardCharsets.UTF_16
            case Some(_) => StandardCharsets.UTF_8
            case None => StandardCharsets.UTF_8
          }.toOption)

      case List(ct) => (Some(ct), None)
      case _ => (None, None)
    }
  }

  override def port: Int = Configs.CAT_HTTP_PORT

  override def host: String = "0.0.0.0"

  initialize()

}

object CatalinaHttp extends CatalinaHttpBase
