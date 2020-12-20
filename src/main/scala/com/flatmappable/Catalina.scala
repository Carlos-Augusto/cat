package com.flatmappable

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.UUID

import com.flatmappable.util.Timer
import com.typesafe.scalalogging.Logger
import com.ubirch.protocol.Protocol
import com.ubirch.protocol.codec.MsgPackProtocolDecoder
import org.apache.commons.codec.binary.Hex
import org.backuity.clist._
import org.backuity.clist.util.Read
import org.backuity.clist.util.Read.reads
import org.json4s.jackson.JsonMethods._
import org.slf4j.LoggerFactory

import scala.util.Try

object Catalina {

  @transient
  protected lazy val logger: Logger =
    Logger(LoggerFactory.getLogger(getClass.getName.split("\\$").headOption.getOrElse("Catalina")))

  implicit val uuidRead: Read[UUID] = reads("a UUID") { UUID.fromString }

  object RegisterKey extends Command(description = "Creates and registers public key") {
    var uuid = arg[UUID](description = "UUID for the identity")
  }

  object RegisterRandomKey extends Command(description = "Creates and registers public key based on a random uuid that is generated internally")

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

  def main(args: Array[String]): Unit = {

    init()

    Cli.parse(args)
      .withProgramName("catalina")
      .withDescription(
        "Tool to use the Ubirch Trust Service",
        Some("With this tool, you can interact with the core-features of the Ubirch Trust Platform (Cloud). \n" +
          "You are able to send micro-certificates from different sources, files, user input, fixed strings. \n" +
          "You can verify the micro-certificates after sending, which guaranties that your timestamp is now immutable and trust-enabled. \n\n" +
          "To modify the target stage or environment , run: export CAT_ENV=dev | demo | prod ")
      )
      .version(version)
      .withCommands(RegisterRandomKey, RegisterKey, GenerateRandomTimestamp, CreateTimestamp, VerifyTimestamp) match {

        case Some(GenerateRandomTimestamp) =>

          logger.info("Generating random UPP for uuid={}", GenerateRandomTimestamp.uuid)

          DataGenerator
          .generate(GenerateRandomTimestamp.uuid, GenerateRandomTimestamp.privateKey, Protocol.Format.MSGPACK)
          .foreach { x =>
            logger.info("upp={}", x.upp)
            logger.info("hash={}", x.hash)
            if (GenerateRandomTimestamp.anchor && GenerateRandomTimestamp.password.nonEmpty) {
              val resp = DataSending.send(uuid = x.UUID, password = GenerateRandomTimestamp.password, hash = x.hash, upp = x.upp)
              printStatus(resp.status)
            }
          }

        case Some(RegisterRandomKey) =>

          val uuid = UUID.randomUUID()

          logger.info("Registering key for uuid={}", uuid)

          val (fullPrivKey, pubKey, privKey, (info, data, resp)) = KeyRegistration.newRegistration(uuid)
          logger.info("\n pub-key={} \n priv-key={} \n priv-key-full={}", pubKey, privKey, fullPrivKey)
          KeyRegistration.logOutput(info, data, resp)

        case Some(RegisterKey) =>

          logger.info("Registering key for uuid={}", RegisterKey.uuid)

          val (fullPrivKey, pubKey, privKey, (info, data, resp)) = KeyRegistration.newRegistration(RegisterKey.uuid)
          logger.info("\n pub-key={} \n priv-key={} \n priv-key-full={}", pubKey, privKey, fullPrivKey)
          KeyRegistration.logOutput(info, data, resp)

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

            val (pmo, upp, hash) = DataGenerator.single(CreateTimestamp.uuid, data, CreateTimestamp.privateKey, Protocol.Format.MSGPACK, CreateTimestamp.withNonce)

            logger.info("pm={}", pmo.toString)
            logger.info("upp={}", toBase64AsString(Hex.decodeHex(upp)))
            logger.info("upp={}", upp)
            logger.info("signed={}", toBase64AsString(pmo.getSigned))
            logger.info("hash={}", hash)

            val timedResp = Timer.time(DataSending.send(uuid = CreateTimestamp.uuid, password = CreateTimestamp.password, hash = hash, upp = upp), "UPP Sending")
            val resp = timedResp.getResult

            val pm = Try(MsgPackProtocolDecoder.getDecoder.decode(resp.body).toString)
              .getOrElse(new String(resp.body, StandardCharsets.UTF_8))

            printStatus(resp.status)
            logger.info("Response Headers: " + resp.headers.toList.mkString(", "))
            logger.info("Response BodyHex: " + Hex.encodeHexString(resp.body))
            logger.info("Response Body: " + pm)
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

          printStatus(resp.status)

          if (resp.status >= OK && resp.status < MULTIPLE_CHOICE) {
            logger.info("\n" + pretty(parse(resp.body)))
          }

        case _ =>

      }
  }
}
