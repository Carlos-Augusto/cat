package com.flatmappable

import java.io.File
import java.nio.file.{ Files, Paths }
import java.util.UUID

import com.flatmappable.util.{ Configs, HttpHelpers, Timer }
import com.typesafe.scalalogging.Logger
import com.ubirch.protocol.Protocol
import com.ubirch.protocol.codec.MsgPackProtocolDecoder
import org.apache.commons.codec.binary.Hex
import org.apache.http.util.EntityUtils
import org.backuity.clist._
import org.backuity.clist.util.Read
import org.backuity.clist.util.Read.reads
import org.json4s.jackson.JsonMethods._
import org.slf4j.LoggerFactory

object Catalina {

  @transient
  protected lazy val logger: Logger =
    Logger(LoggerFactory.getLogger(getClass.getName.split("\\$").headOption.getOrElse("Catalina")))

  implicit val uuidRead: Read[UUID] = reads("a UUID") { UUID.fromString }

  object RegisterKey extends Command(description = "Creates and registers public key") {
    var uuid = arg[UUID](description = "UUID for the identity")
  }

  object GenerateRandomTimestamp extends Command(description = "Creates a random upp and hash") {
    var uuid = arg[UUID](description = "UUID for the identity")
    var privateKey = arg[String](description = "Private Key for UUID for the registered identity")
    var anchor = opt[Boolean](description = "Anchor", abbrev = "-a", default = false)
    var password = opt[String](description = "Password for your identity, only needed when 'Anchoring'", default = "")

    validate {
      if (moreThanOne(anchor, password.isEmpty)) {
        parsingError("Use --password too please")
      }
    }

  }

  object CreateTimestamp extends Command(description = "Creates a secure timestamp from the  provided using the ubirch network") {
    var readLine = opt[Boolean](description = "Read data from console", default = false, abbrev = "-l")
    var withNonce = opt[Boolean](description = "Add a nonce to the message digest", default = false, abbrev = "-n")
    var text = opt[String](description = "The text you would like to timestamp", default = "")
    var file = opt[File](description = "The path to the file to timestamp", default = new File(""))
    var uuid = arg[UUID](description = "UUID for the identity")
    var password = arg[String](description = "Password for your identity")
    var privateKey = arg[String](description = "Private Key for UUID for the registered identity")

    validate {
      if (moreThanOne(text.nonEmpty, file.getName.nonEmpty, readLine)) {
        parsingError("Use --text or --file or --l")
      }
    }

  }

  object VerifyTimestamp extends Command(description = "Lists the secure timestamps that have been created") {
    var hash = arg[String](description = "The hash to verify", required = true)
    var simple = opt[Boolean](description = "Simple verification.", default = false, abbrev = "-s")
    var initial = opt[Boolean](description = "Initial verification.", default = false, abbrev = "-i")
    var upper = opt[Boolean](description = "Upper verification.", default = false, abbrev = "-u")
    var full = opt[Boolean](description = "Full verification.", default = false, abbrev = "-f")

    validate {
      if (moreThanOne(simple, initial, upper, full)) {
        parsingError("Use one single option at a time.")
      }
    }

  }

  def main(args: Array[String]) {

    logger.info("Environment={}", Configs.ENV)

    val home = Paths.get(System.getProperty("user.home") + "/.cat/")
    if (!home.toFile.exists()) {
      Files.createDirectory(home)
    }

    Cli.parse(args)
      .withProgramName("catalina")
      .version("1.1.0")
      .withCommands(RegisterKey, GenerateRandomTimestamp, CreateTimestamp, VerifyTimestamp) match {

        case Some(GenerateRandomTimestamp) =>

          logger.info("Generating random UPP for uuid={}", GenerateRandomTimestamp.uuid)

          DataGenerator
          .generate(GenerateRandomTimestamp.uuid, GenerateRandomTimestamp.privateKey, Protocol.Format.MSGPACK)
          .foreach { x =>
            logger.info("upp={}", x.upp)
            logger.info("hash={}", x.hash)
            if (GenerateRandomTimestamp.anchor && GenerateRandomTimestamp.password.nonEmpty) {
              val resp = DataSending.send(x.UUID, GenerateRandomTimestamp.password, DataGenerator.toBytesFromHex(x.upp))
              HttpHelpers.printStatus(resp.getStatusLine.getStatusCode)
            }
          }

        case Some(RegisterKey) =>

          logger.info("Registering key for uuid={}", RegisterKey.uuid)

          val (pubKey, privKey, (info, data, verification, resp, body)) = KeyRegistration.newRegistration(RegisterKey.uuid)
          logger.info("pub-key={} priv-key={}", pubKey, privKey)
          KeyRegistration.logOutput(info, data, verification, resp, body)

        case Some(CreateTimestamp) =>

          var source = ""
          val data = {
            if (CreateTimestamp.readLine) {
              logger.info("Please enter your data (end with 'return')'")
              source = "line"
              scala.io.StdIn.readLine()
            } else if (CreateTimestamp.file.getName.nonEmpty) {
              source = "file"
              logger.info(s"$source={}", CreateTimestamp.file.getName)
              val bytes = if (CreateTimestamp.file.exists() && CreateTimestamp.file.isFile) {
                Files.readAllBytes(CreateTimestamp.file.toPath)
              } else {
                logger.info(s"$source={} doesn't exist", CreateTimestamp.file.getName)
                Array.empty[Byte]
              }

              Hex.encodeHexString(bytes)

            } else {
              source = "text"
              CreateTimestamp.text
            }
          }

          logger.info(s"Creating Timestamp($source) for uuid={}", CreateTimestamp.uuid)

          if (data.nonEmpty) {
            logger.info(s"$source={}", data)

            val (_, upp, hash) = DataGenerator.single(CreateTimestamp.uuid, data, CreateTimestamp.privateKey, Protocol.Format.MSGPACK, CreateTimestamp.withNonce)

            logger.info("upp={}", upp)
            logger.info("hash={}", hash)

            val timedResp = Timer.time(DataSending.send(CreateTimestamp.uuid, CreateTimestamp.password, DataGenerator.toBytesFromHex(upp)), "UPP Sending")
            val resp = timedResp.getResult

            val bytes = EntityUtils.toByteArray(resp.getEntity)
            val pm = MsgPackProtocolDecoder.getDecoder.decode(bytes)

            HttpHelpers.printStatus(resp.getStatusLine.getStatusCode)
            logger.info("Response Headers: " + resp.getAllHeaders.toList.mkString(", "))
            logger.info("Response BodyHex: " + Hex.encodeHexString(bytes))
            logger.info("Response Body: " + pm.toString)
            logger.info("Response Time: (ms)" + timedResp.elapsed)

          } else {
            logger.warn(s"$source data is not valid. Could be empty or file doesn't exist.")
          }

        case Some(VerifyTimestamp) =>

          logger.info("Verifying timestamp for hash={}", VerifyTimestamp.hash)

          val resp = if (VerifyTimestamp.initial) {
            VerifyData.simple(VerifyTimestamp.hash)
          } else if (VerifyTimestamp.simple) {
            VerifyData.initial(VerifyTimestamp.hash)
          } else if (VerifyTimestamp.upper) {
            VerifyData.upper(VerifyTimestamp.hash)
          } else {
            VerifyData.full(VerifyTimestamp.hash)
          }

          HttpHelpers.printStatus(resp.getStatusLine.getStatusCode)

          val body = HttpHelpers.readEntityAsJValue(resp)

          logger.info(pretty(body))

        case _ =>

      }
  }
}
