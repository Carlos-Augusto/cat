package com

import java.nio.file.{ Files, Path, Paths, StandardOpenOption }
import java.text.SimpleDateFormat
import java.util.{ Base64, TimeZone }

import com.flatmappable.util.Configs
import com.typesafe.scalalogging.LazyLogging
import org.apache.commons.codec.binary.Hex
import org.json4s.jackson.JsonMethods.parse
import org.json4s.jackson.Serialization
import org.json4s.{ Formats, NoTypeHints }

package object flatmappable extends LazyLogging {

  implicit lazy val formats: Formats = Serialization.formats(NoTypeHints) ++ org.json4s.ext.JavaTypesSerializers.all

  val PATH_HOME: Path = Paths.get(Configs.DATA_FOLDER).resolve(".cat").normalize()
  val PATH_UPPs: Path = PATH_HOME.resolve(".sent_upps").normalize()
  val PATH_KEYS: Path = PATH_HOME.resolve(".keys").normalize()

  val defaultDataFormat: SimpleDateFormat = {
    val _df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    _df.setTimeZone(TimeZone.getTimeZone("UTC"))
    _df
  }

  def init(): Unit = {
    logger.info("Environment={}", Configs.ENV)
    if (!PATH_HOME.toFile.exists()) {
      logger.info("Creating home=" + PATH_HOME.toFile.toString)
      Files.createDirectory(PATH_HOME)
    } else {
      logger.info("home exists=" + PATH_HOME.toFile.getCanonicalPath)
    }
  }

  def store(dataToStore: Array[Byte], path: Path, responseStatus: Int): Unit = {
    if (responseStatus >= 200 && responseStatus < 300) {
      Files.write(path, dataToStore, StandardOpenOption.CREATE, StandardOpenOption.APPEND)
    }
  }

  def toBase64AsString(data: Array[Byte]): String = Base64.getEncoder.encodeToString(data)

  def toHex(data: Array[Byte]): String = Hex.encodeHexString(data)

  def toBytesFromHex(data: String): Array[Byte] = Hex.decodeHex(data)

  def readEntityAsJValue(response: String) = parse(response)

  def printStatus(status: Int) = {
    if (status < 299)
      logger.info("Response Status: " + Console.GREEN + status + Console.RESET)
    else
      logger.info("Response Status: " + Console.RED + status + Console.RESET)

  }

}
