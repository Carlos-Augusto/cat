package com.flatmappable.util

import com.typesafe.scalalogging.LazyLogging
import org.json4s.jackson.JsonMethods.parse

object HttpHelpers extends LazyLogging {

  def readEntityAsJValue(response: String) = parse(response)

  def printStatus(status: Int) = {
    if (status < 299)
      logger.info("Response Status: " + Console.GREEN + status + Console.RESET)
    else
      logger.info("Response Status: " + Console.RED + status + Console.RESET)

  }

}
