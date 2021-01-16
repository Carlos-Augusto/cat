package com.flatmappable

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.UUID

import com.flatmappable.util.{ Logging, ResponseData, Timer }
import com.ubirch.protocol.Protocol
import com.ubirch.protocol.codec.MsgPackProtocolDecoder
import org.apache.commons.codec.binary.Hex
import org.backuity.clist._
import org.backuity.clist.util.Read
import org.backuity.clist.util.Read.reads
import org.json4s.jackson.JsonMethods._

import scala.util.{ Failure, Random, Success, Try }

object Catalina extends Logging {

  implicit val uuidRead: Read[UUID] = reads("a UUID") { UUID.fromString }

  object RegisterKey extends Command(description = "Creates and registers public key") {
    var uuid = arg[UUID](description = "UUID for the identity")

    def run() = {
      logger.info("Registering key for uuid={}", RegisterKey.uuid)

      val (fullPrivKey, pubKey, privKey, (info, data, resp)) = KeyRegistration.newRegistration(RegisterKey.uuid)
      logger.info("\n pub-key={} \n priv-key={} \n priv-key-full={}", pubKey, privKey, fullPrivKey)
      logOutput(info, data, resp)
    }
  }

  object RegisterRandomKey extends Command(description = "Creates and registers public key based on a random uuid that is generated internally") {
    def run() = {
      val uuid = UUID.randomUUID()

      logger.info("Registering key for uuid={}", uuid)

      val (fullPrivKey, pubKey, privKey, (info, data, resp)) = KeyRegistration.newRegistration(uuid)
      logger.info("\n pub-key={} \n priv-key={} \n priv-key-full={}", pubKey, privKey, fullPrivKey)
      logOutput(info, data, resp)
    }
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

    def run() = {
      logger.info("Generating random UPP for uuid={}", GenerateRandomTimestamp.uuid)

      DataGenerator
        .buildMessage(GenerateRandomTimestamp.uuid, Random.nextBytes(64), GenerateRandomTimestamp.privateKey, Protocol.Format.MSGPACK, withNonce = true)
        .foreach { x =>
          logger.info("upp={}", x.uppAsHex)
          logger.info("hash={}", x.hashAsBase64)
          if (GenerateRandomTimestamp.anchor && GenerateRandomTimestamp.password.nonEmpty) {
            val resp = DataSending.send(uuid = x.UUID, password = GenerateRandomTimestamp.password, hash = x.hashAsBase64, upp = x.uppAsHex)
            printStatus(resp.status)
          }
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

    def run() = {
      var source = ""
      val data: Array[Byte] = {
        if (CreateTimestamp.readLine) {
          logger.info("Please enter your data (end with 'return')'")
          source = "line"
          scala.io.StdIn.readLine().getBytes(StandardCharsets.UTF_8)
        } else if (CreateTimestamp.file.getName.nonEmpty) {
          source = "file"
          logger.info(s"$source={}", CreateTimestamp.file.getName)
          if (CreateTimestamp.file.exists() && CreateTimestamp.file.isFile) {
            Files.readAllBytes(CreateTimestamp.file.toPath)
          } else {
            logger.info(s"$source={} doesn't exist", CreateTimestamp.file.getName)
            Array.empty[Byte]
          }
        } else {
          source = "text"
          CreateTimestamp.text.getBytes(StandardCharsets.UTF_8)
        }
      }

      logger.info(s"Creating Timestamp($source) for uuid={}", CreateTimestamp.uuid)

      if (data.nonEmpty) {
        source match {
          case "file" => //print nothing
          case _ => logger.info(s"$source={}", new String(data, StandardCharsets.UTF_8))
        }

        DataGenerator.buildMessage(CreateTimestamp.uuid, data, CreateTimestamp.privateKey, Protocol.Format.MSGPACK, CreateTimestamp.withNonce) match {
          case Failure(exception) =>
            logger.info("Error creating protocol message " + exception.getMessage)
          case Success(sd) =>

            logger.info("pm={}", sd.protocolMessage.toString)
            logger.info("upp={}", sd.uppAsHex)
            logger.info("signed={}", toBase64AsString(sd.protocolMessage.getSigned))
            logger.info("hash={}", sd.hashAsBase64)

            val timedResp = Timer.time(DataSending.send(uuid = CreateTimestamp.uuid, password = CreateTimestamp.password, hash = sd.hashAsBase64, upp = sd.uppAsHex), "UPP Sending")
            val resp = timedResp.getResult

            val pm = Try(MsgPackProtocolDecoder.getDecoder.decode(resp.body).toString)
              .getOrElse(new String(resp.body, StandardCharsets.UTF_8))

            logger.info("Response Headers: " + resp.headers.toList.mkString(", "))
            logger.info("Response BodyHex: " + Hex.encodeHexString(resp.body))
            logger.info("Response Body: " + pm)
            logger.info("Response Time: (ms)" + timedResp.elapsed)
            printStatus(resp.status)

        }

      } else {
        logger.warn(s"$source data is not valid. Could be empty or file doesn't exist.")
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

    def run() = {
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
    }

  }

  def logOutput(info: String, data: String, resp: ResponseData[String]): Unit = {
    logger.info("Info: " + info)
    logger.info("Data: " + data)
    logger.info("Response: " + resp.body)
    printStatus(resp.status)
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

        case Some(GenerateRandomTimestamp) => GenerateRandomTimestamp.run()
        case Some(RegisterRandomKey) => RegisterRandomKey.run()
        case Some(RegisterKey) => RegisterKey.run()
        case Some(CreateTimestamp) => CreateTimestamp.run()
        case Some(VerifyTimestamp) => VerifyTimestamp.run()
        case _ =>

      }
  }
}
