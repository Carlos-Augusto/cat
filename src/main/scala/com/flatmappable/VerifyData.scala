package com.flatmappable

import com.flatmappable.util.EnvConfigs
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients

object VerifyData {

  val client: HttpClient = HttpClients.createMinimal()

  def verifyKeyRequest(url: String, hash: String) = {
    val regRequest = new HttpPost(url)
    regRequest.setEntity(new StringEntity(hash))
    regRequest
  }

  def simple(hash: String) = {
    client.execute(verifyKeyRequest("https://verify." + EnvConfigs.ENV + ".ubirch.com/api/upp", hash))
  }

  def initial(hash: String) = {
    client.execute(verifyKeyRequest("https://verify." + EnvConfigs.ENV + ".ubirch.com/api/upp/verify", hash))
  }

  def upper(hash: String) = {
    client.execute(verifyKeyRequest("https://verify." + EnvConfigs.ENV + ".ubirch.com/api/upp/verify/anchor", hash))
  }

  def full(hash: String) = {
    client.execute(verifyKeyRequest("https://verify." + EnvConfigs.ENV + ".ubirch.com/api/upp/verify/record", hash))
  }

}
