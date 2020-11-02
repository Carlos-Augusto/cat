package com.flatmappable

import java.nio.charset.StandardCharsets
import java.util.UUID

import com.flatmappable.util.{ Configs, RequestClient, ResponseData }
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ByteArrayEntity

object DataSending extends RequestClient {

  def sendKeyRequest(uuid: UUID, password: String, body: Array[Byte], extraHeaders: Map[String, Seq[String]]): HttpPost = {
    val regRequest = new HttpPost("https://ubproxy.enchain.it/ubproxy/api/v1/upp")
    regRequest.setHeader(CONTENT_TYPE, "application/octet-stream")
    regRequest.setHeader(X_UBIRCH_HARDWARE_ID, uuid.toString)
    regRequest.setHeader(X_UBIRCH_AUTH_TYPE, "ubirch")
    regRequest.setHeader(X_UBIRCH_CREDENTIAL, password)

    extraHeaders.foreach { case (k, v) =>
      v.foreach { vx => regRequest.addHeader(k, vx) }
    }

    regRequest.setEntity(new ByteArrayEntity(body))
    regRequest
  }

  def send(uuid: UUID, password: String, hash: String, upp: String, extraHeaders: Map[String, Seq[String]]): ResponseData[Array[Byte]] = {
    val response = call(sendKeyRequest(uuid, password, toBytesFromHex(upp), extraHeaders))

    store(
      s"${Configs.ENV},$uuid,$hash,$upp\n".getBytes(StandardCharsets.UTF_8),
      PATH_UPPs,
      response.status
    )

    response
  }

  def send(uuid: UUID, password: String, hash: String, upp: String): ResponseData[Array[Byte]] = {
    send(uuid, password, hash, upp, Map.empty)
  }

}
