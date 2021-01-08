package com.flatmappable

import com.flatmappable.util._
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity

object KeyCreation extends RequestClient {

  def registerKeyRequest(body: String): HttpPost = {
    val regRequest = new HttpPost(Configs.KEY_REGISTRATION_URL)
    regRequest.setHeader(CONTENT_TYPE, "application/json")
    regRequest.setEntity(new StringEntity(body))
    regRequest
  }

  def create(key: String): ResponseData[String] = {
    callAsString(registerKeyRequest(key))
  }

}
