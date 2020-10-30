package com.flatmappable

import java.nio.charset.StandardCharsets
import java.util.UUID

import com.flatmappable.util.{ Configs, RequestClient }
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ByteArrayEntity

object DataSending extends RequestClient {

  def sendKeyRequest(uuid: UUID, password: String, token: String, body: Array[Byte]) = {
    val regRequest = new HttpPost("http://ubproxy.enchain.it:3000/ubproxy/api/v1/upp")
    regRequest.setHeader(CONTENT_TYPE, "application/octet-stream")
    regRequest.setHeader(X_UBIRCH_HARDWARE_ID, uuid.toString)
    regRequest.setHeader(X_UBIRCH_AUTH_TYPE, "ubirch")
    regRequest.setHeader(X_UBIRCH_CREDENTIAL, password)
    regRequest.setHeader(X_TOKEN, token)
    regRequest.setEntity(new ByteArrayEntity(body))
    regRequest
  }

  def send(uuid: UUID, password: String, token: String, hash: String, upp: String) = {
    val response = call(sendKeyRequest(uuid, password, token, toBytesFromHex(upp)))

    store(
      s"${Configs.ENV},$uuid,$hash,$upp\n".getBytes(StandardCharsets.UTF_8),
      PATH_UPPs,
      response.status
    )

    response
  }

}
