package com

import java.nio.file.{ Files, Path, Paths }
import java.text.SimpleDateFormat
import java.time.Clock
import java.util.{ Base64, TimeZone }

import com.flatmappable.util.Configs
import com.typesafe.scalalogging.Logger
import org.apache.commons.codec.binary.Hex
import org.joda.time.{ DateTime, DateTimeZone }
import org.json4s.jackson.Serialization
import org.json4s.{ Formats, NoTypeHints }
import org.slf4j.LoggerFactory

package object flatmappable extends DataStore {

  @transient
  protected lazy val logger: Logger =
    Logger(LoggerFactory.getLogger(getClass.getName.split("\\$").headOption.getOrElse("flatmappable")))

  implicit lazy val formats: Formats = Serialization.formats(NoTypeHints) ++ org.json4s.ext.JavaTypesSerializers.all

  final val version = "0.0.7"

  final val clock: Clock = Clock.systemUTC

  final val PATH_HOME: Path = Paths.get(Configs.DATA_FOLDER).resolve(".cat").normalize()

  final val defaultDataFormat: SimpleDateFormat = {
    val _df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    _df.setTimeZone(TimeZone.getTimeZone("UTC"))
    _df
  }

  def store[T](responseStatus: Int)(f: => T): Option[T] = {
    if (responseStatus >= OK && responseStatus < MULTIPLE_CHOICE) {
      Option(f)
    } else {
      None
    }
  }

  def init(http: Boolean = false): Unit = {
    if (http) {
      logger.info("Port={}", Configs.CAT_HTTP_PORT)
    }
    logger.info("Environment={}", Configs.ENV.name)
    if (!PATH_HOME.toFile.exists()) {
      logger.info("Creating home=" + PATH_HOME.toFile.toString)
      Files.createDirectory(PATH_HOME)
    } else {
      logger.info("home exists=" + PATH_HOME.toFile.getCanonicalPath)
    }

    if (pending()) {
      val _ = migrate()
    }

  }

  def toBase64AsString(data: Array[Byte]): String = Base64.getEncoder.encodeToString(data)

  def toHex(data: Array[Byte]): String = Hex.encodeHexString(data)

  def toBytesFromHex(data: String): Array[Byte] = Hex.decodeHex(data)

  def printStatus(status: Int): Unit = {
    if (status < MULTIPLE_CHOICE)
      logger.info("Response Status: " + Console.GREEN + status + Console.RESET)
    else
      logger.info("Response Status: " + Console.RED + status + Console.RESET)

  }

  def closableTry[A, B](resource: => A)(cleanup: A => Unit)(code: A => B): Either[Exception, B] = {
    try {
      val r = resource
      try { Right(code(r)) } finally { cleanup(r) }
    } catch { case e: Exception => Left(e) }
  }

  def dateTimeNowUTC: DateTime = DateTime.now(DateTimeZone.UTC)
  def now: Long = clock.millis()

  def OK: Int = 200
  def MULTIPLE_CHOICE = 300
  def BAD_REQUEST = 400
  def UNAUTHORIZED = 401
  def CONFLICT = 409
  def KNOWN_UPP: Int = CONFLICT
  def INTERNAL_SERVER_ERROR: Int = 500

  def CONTENT_TYPE = "Content-Type"
  def X_UBIRCH_HARDWARE_ID = "X-Ubirch-Hardware-Id"
  def X_UBIRCH_AUTH_TYPE = "X-Ubirch-Auth-Type"
  def X_UBIRCH_CREDENTIAL = "X-Ubirch-Credential"

}
