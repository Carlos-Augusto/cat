package com.flatmappable

import com.flatmappable.util.{ Configs, RequestClient }
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity

object VerifyData extends RequestClient {

  def verifyKeyRequest(url: String, hash: String) = {
    val regRequest = new HttpPost(url)
    regRequest.setEntity(new StringEntity(hash))
    regRequest
  }

  def simple(hash: String) = {
    callAsString(verifyKeyRequest("https://verify." + Configs.ENV + ".ubirch.com/api/upp", hash))
  }

  def initial(hash: String) = {
    callAsString(verifyKeyRequest("https://verify." + Configs.ENV + ".ubirch.com/api/upp/verify", hash))
  }

  def upper(hash: String) = {
    callAsString(verifyKeyRequest("https://verify." + Configs.ENV + ".ubirch.com/api/upp/verify/anchor", hash))
  }

  def full(hash: String) = {
    callAsString(verifyKeyRequest("https://verify." + Configs.ENV + ".ubirch.com/api/upp/verify/record", hash))
  }

}
