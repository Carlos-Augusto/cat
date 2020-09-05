package com.flatmappable

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Paths, StandardOpenOption }
import java.util.UUID

import com.flatmappable.util.Configs
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ByteArrayEntity
import org.apache.http.impl.client.HttpClients

object DataSending {

  val client: HttpClient = HttpClients.createMinimal()

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
    val response = client.execute(sendKeyRequest(uuid, password, DataGenerator.toBytesFromHex(upp)))

    if (response.getStatusLine.getStatusCode <= 200 || response.getStatusLine.getStatusCode <= 200) {
      val keyLineToSave = s"${Configs.ENV},$uuid,$hash,$upp\n".getBytes(StandardCharsets.UTF_8)
      Files.write(Paths.get(System.getProperty("user.home") + "/.cat/.sent_upps"), keyLineToSave, StandardOpenOption.CREATE, StandardOpenOption.APPEND)
    }

    response

  }

}
