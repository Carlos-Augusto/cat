package com.flatmappable

import com.flatmappable.util.{ Configs, RequestClient, ResponseData }
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity

object VerifyData extends RequestClient {

  def verifyKeyRequest(url: String, hash: String): HttpPost = {
    val regRequest = new HttpPost(url)
    regRequest.setEntity(new StringEntity(hash))
    regRequest
  }

  def simple(hash: String): ResponseData[String] = {
    callAsString(verifyKeyRequest(Configs.SIMPLE_VERIFICATION_URL, hash))
  }

  def initial(hash: String): ResponseData[String] = {
    callAsString(verifyKeyRequest(Configs.INITIAL_VERIFICATION_URL, hash))
  }

  def upper(hash: String): ResponseData[String] = {
    callAsString(verifyKeyRequest(Configs.UPPER_VERIFICATION_URL, hash))
  }

  def full(hash: String): ResponseData[String] = {
    callAsString(verifyKeyRequest(Configs.FULL_VERIFICATION_URL, hash))
  }

}
