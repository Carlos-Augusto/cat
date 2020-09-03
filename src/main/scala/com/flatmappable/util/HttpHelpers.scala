package com.flatmappable.util

import org.apache.http.HttpResponse
import org.apache.http.util.EntityUtils
import org.json4s.jackson.JsonMethods.parse

object HttpHelpers {

  def readEntity(response: HttpResponse) = {
    EntityUtils.toString(response.getEntity)
  }

  def readEntityAsJValue(response: HttpResponse) = {
    parse(readEntity(response))
  }

}
