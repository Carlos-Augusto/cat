package com.flatmappable.util

import com.typesafe.scalalogging.{ LazyLogging, Logger }
import org.slf4j.LoggerFactory

trait Logging extends LazyLogging {

  @transient
  override protected lazy val logger: Logger =
    Logger(LoggerFactory.getLogger(getClass.getName.split("\\$").headOption.getOrElse("Catalina")))

}
