package com.flatmappable

import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.{ Base64, UUID }

import com.flatmappable.util._
import com.typesafe.scalalogging.LazyLogging
import com.ubirch.crypto.PrivKey
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.json4s.jackson.JsonMethods._

object KeyRegistration extends RequestClient with LazyLogging {

  def pubKeyInfoData(
      algorithm: String,
      created: String,
      hwDeviceId: String,
      pubKey: String,
      pubKeyId: String,
      validNotAfter: String,
      validNotBefore: String
  ): String = {
    s"""
       |{
       |   "algorithm": "$algorithm",
       |   "created": "$created",
       |   "hwDeviceId": "$hwDeviceId",
       |   "pubKey": "$pubKey",
       |   "pubKeyId": "$pubKeyId",
       |   "validNotAfter": "$validNotAfter",
       |   "validNotBefore": "$validNotBefore"
       |}
    """.stripMargin
  }

  def pubKeyInfoData(clientUUID: UUID, df: SimpleDateFormat, sk: String, created: Long): String = {
    pubKeyInfoData(
      algorithm = "ECC_ED25519",
      created = df.format(created),
      hwDeviceId = clientUUID.toString,
      pubKey = sk,
      pubKeyId = sk,
      validNotAfter = df.format(created + 31557600000L),
      validNotBefore = df.format(created)
    )
  }

  def registrationData(pubKeyInfoData: String, signature: String): String = {
    s"""
      |{
      |   "pubKeyInfo": $pubKeyInfoData,
      |   "signature": "$signature"
      |}
    """.stripMargin
  }

  def registerKeyRequest(body: String): HttpPost = {
    val regRequest = new HttpPost(Configs.KEY_REGISTRATION_URL)
    regRequest.setHeader(CONTENT_TYPE, "application/json")
    regRequest.setEntity(new StringEntity(body))
    regRequest
  }

  def getKey(privateKey: String): PrivKey = {
    val clientKeyBytes = Base64.getDecoder.decode(privateKey)
    KeyPairHelper.privateKeyEd25519(clientKeyBytes)
  }

  def register(UUID: UUID, publicKey: String, privateKey: String) = {

    val clientKey = getKey(privateKey)

    val info = compact(parse(pubKeyInfoData(UUID, defaultDataFormat, publicKey, now)))
    val signature = clientKey.sign(info.getBytes(StandardCharsets.UTF_8))
    val data = compact(parse(registrationData(info, toBase64AsString(signature))))

    val verification = clientKey.verify(info.getBytes, signature)
    val resp = callAsString(registerKeyRequest(data))

    (info, data, verification, resp)
  }

  def logOutput(info: String, data: String, verification: Boolean, resp: ResponseData[String]): Unit = {
    logger.info("Info: " + info)
    logger.info("Data: " + data)
    logger.info("Verification: " + verification.toString)
    logger.info("Response: " + resp.body)
    printStatus(resp.status)
  }

  def newRegistration(uuid: UUID) = {
    val (key, pubKey, privKey) = KeyPairHelper.asString(KeyPairHelper.privateKeyEd25519)
    val response = register(uuid, pubKey, privKey)

    store(
      s"${Configs.ENV},$uuid,ECC_ED25519,$pubKey,$privKey,$key\n".getBytes(StandardCharsets.UTF_8),
      PATH_KEYS,
      response._4.status
    )

    (key, pubKey, privKey, response)

  }

}
