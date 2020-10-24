package com

import java.nio.file.{ Files, Path, Paths, StandardOpenOption }
import java.util.Base64

import com.flatmappable.util.Configs
import com.typesafe.scalalogging.LazyLogging
import org.apache.commons.codec.binary.Hex

package object flatmappable extends LazyLogging {

  val PATH_HOME: Path = Paths.get(Configs.DATA_FOLDER).resolve(".cat").normalize()
  val PATH_UPPs: Path = PATH_HOME.resolve(".sent_upps").normalize()
  val PATH_KEYS: Path = PATH_HOME.resolve(".keys").normalize()

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

}
