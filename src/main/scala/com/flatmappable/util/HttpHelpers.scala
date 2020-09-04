package com.flatmappable.util

import com.flatmappable.Catalina.logger
import com.typesafe.scalalogging.LazyLogging
import org.apache.http.HttpResponse
import org.apache.http.util.EntityUtils
import org.json4s.jackson.JsonMethods.parse

object HttpHelpers extends LazyLogging {

  def readEntity(response: HttpResponse) = {
    EntityUtils.toString(response.getEntity)
  }

  def readEntityAsJValue(response: HttpResponse) = {
    parse(readEntity(response))
  }

  def printStatus(status: Int) = {
    if (status < 299)
      logger.info("Response Status: " + Console.GREEN + status + Console.RESET)
    else
      logger.info("Response Status: " + Console.RED + status + Console.RESET)

  }

}
