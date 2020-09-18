package com

import java.nio.file.{ Files, Path, Paths, StandardOpenOption }

import com.flatmappable.util.Configs
import com.typesafe.scalalogging.LazyLogging

package object flatmappable extends LazyLogging {

  def init(): Unit = {
    logger.info("Environment={}", Configs.ENV)

    val home = Paths.get(".cat")
    if (!home.toFile.exists()) {
      Files.createDirectory(home)
    }
  }

  def store(dataToStore: Array[Byte], path: Path, responseStatus: Int): Unit =
    if (responseStatus >= 200 && responseStatus < 300) {
      Files.write(path, dataToStore, StandardOpenOption.CREATE, StandardOpenOption.APPEND)
    }

}
