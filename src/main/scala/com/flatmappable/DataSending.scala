package com.flatmappable

import java.util.UUID

import com.flatmappable.util.{ Configs, RequestClient, ResponseData }
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ByteArrayEntity
import org.joda.time.DateTime

object DataSending extends RequestClient {

  def sendUPP(uuid: UUID, password: String, body: Array[Byte], extraHeaders: Map[String, Seq[String]]): HttpPost = {
    val regRequest = new HttpPost(Configs.DATA_SENDING_URL)
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
    val response = call(sendUPP(uuid, password, toBytesFromHex(upp), extraHeaders))

    doWhenOK(response.status) {
      Timestamps.insert(
        TimestampRow(
          id = UUID.randomUUID(),
          env = Configs.ENV,
          uuid = uuid,
          hash = hash,
          upp = upp,
          createdAt = new DateTime()
        )
      )
    }

    response
  }

  def send(uuid: UUID, password: String, hash: String, upp: String): ResponseData[Array[Byte]] = {
    send(uuid, password, hash, upp, Map.empty)
  }

}
