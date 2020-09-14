package com.flatmappable

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Paths, StandardOpenOption }
import java.util.UUID

import com.flatmappable.util.{ Configs, RequestClient }
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ByteArrayEntity

object DataSending extends RequestClient {

  def sendKeyRequest(uuid: UUID, password: String, body: Array[Byte]) = {
    val regRequest = new HttpPost("https://niomon." + Configs.ENV + ".ubirch.com")
    regRequest.setHeader("Content-Type", "application/octet-stream")
    regRequest.setHeader("X-Ubirch-Hardware-Id", uuid.toString)
    regRequest.setHeader("X-Ubirch-Auth-Type", "ubirch")
    regRequest.setHeader("X-Ubirch-Credential", password)
    regRequest.setEntity(new ByteArrayEntity(body))
    regRequest
  }

  def send(uuid: UUID, password: String, hash: String, upp: String) = {
    val response = call(sendKeyRequest(uuid, password, DataGenerator.toBytesFromHex(upp)))

    if (response.status >= 200 && response.status < 300) {
      val keyLineToSave = s"${Configs.ENV},$uuid,$hash,$upp\n".getBytes(StandardCharsets.UTF_8)
      Files.write(Paths.get(System.getProperty("user.home") + "/.cat/.sent_upps"), keyLineToSave, StandardOpenOption.CREATE, StandardOpenOption.APPEND)
    }

    response

  }

}
