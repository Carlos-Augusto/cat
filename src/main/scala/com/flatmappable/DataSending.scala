package com.flatmappable

import java.nio.charset.StandardCharsets
import java.util.UUID

import com.flatmappable.util.{ Configs, RequestClient }
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ByteArrayEntity

object DataSending extends RequestClient {

  def CONTENT_TYPE = "Content-Type"
  def X_UBIRCH_HARDWARE_ID = "X-Ubirch-Hardware-Id"
  def X_UBIRCH_AUTH_TYPE = "X-Ubirch-Auth-Type"
  def X_UBIRCH_CREDENTIAL = "X-Ubirch-Credential"

  def sendKeyRequest(uuid: UUID, password: String, body: Array[Byte]) = {
    val regRequest = new HttpPost("https://niomon." + Configs.ENV + ".ubirch.com")
    regRequest.setHeader(CONTENT_TYPE, "application/octet-stream")
    regRequest.setHeader(X_UBIRCH_HARDWARE_ID, uuid.toString)
    regRequest.setHeader(X_UBIRCH_AUTH_TYPE, "ubirch")
    regRequest.setHeader(X_UBIRCH_CREDENTIAL, password)
    regRequest.setEntity(new ByteArrayEntity(body))
    regRequest
  }

  def send(uuid: UUID, password: String, hash: String, upp: String) = {
    val response = call(sendKeyRequest(uuid, password, toBytesFromHex(upp)))

    store(
      s"${Configs.ENV},$uuid,$hash,$upp\n".getBytes(StandardCharsets.UTF_8),
      PATH_UPPs,
      response.status
    )

    response
  }

}
