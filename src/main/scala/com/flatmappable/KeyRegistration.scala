package com.flatmappable

import java.nio.charset.StandardCharsets
import java.util.{ Base64, UUID }

import com.flatmappable.util._
import com.flatmappable.util.KeyPairHelper.EnrichedPrivKey
import com.typesafe.scalalogging.LazyLogging
import com.ubirch.crypto.PrivKey
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.json4s.jackson.JsonMethods._

object KeyRegistration extends RequestClient with LazyLogging {

  def ECC_ECDSA: Symbol = Symbol("ECC_ECDSA")
  def ECC_ED25519: Symbol = Symbol("ECC_ED25519")

  def pubKeyInfoData(
      algorithm: Symbol,
      created: String,
      hwDeviceId: String,
      pubKey: String,
      pubKeyId: String,
      validNotAfter: String,
      validNotBefore: String
  ): String = {
    s"""
       |{
       |   "algorithm": "${algorithm.name}",
       |   "created": "$created",
       |   "hwDeviceId": "$hwDeviceId",
       |   "pubKey": "$pubKey",
       |   "pubKeyId": "$pubKeyId",
       |   "validNotAfter": "$validNotAfter",
       |   "validNotBefore": "$validNotBefore"
       |}
    """.stripMargin
  }

  def pubKeyInfoData(clientUUID: UUID, algorithm: Symbol, sk: String, created: Long): String = {
    pubKeyInfoData(
      algorithm = algorithm,
      created = defaultDataFormat.format(created),
      hwDeviceId = clientUUID.toString,
      pubKey = sk,
      pubKeyId = sk,
      validNotAfter = defaultDataFormat.format(created + 31557600000L),
      validNotBefore = defaultDataFormat.format(created)
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

  def create(key: String): ResponseData[String] = {
    callAsString(registerKeyRequest(key))
  }

  def getKey(privateKey: String): PrivKey = {
    val clientKeyBytes = Base64.getDecoder.decode(privateKey)
    KeyPairHelper.privateKeyEd25519(clientKeyBytes)
  }

  def createKey(uuid: UUID, algo: Symbol = ECC_ED25519, clientKey: PrivKey = KeyPairHelper.privateKeyEd25519, created: Long = now) = {
    val pubKey = clientKey.getRawPublicKeyAsString
    val info = compact(parse(pubKeyInfoData(uuid, algo, pubKey, created)))
    val signature = clientKey.sign(info.getBytes(StandardCharsets.UTF_8))
    val data = compact(parse(registrationData(info, toBase64AsString(signature))))
    val verification = clientKey.verify(info.getBytes, signature)

    if (!verification) throw new Exception("Key creation validation failed")

    (clientKey, ECC_ED25519, info, data)

  }

  def newRegistration(uuid: UUID) = {

    val (clientKey, algo, info, data) = createKey(uuid)
    val (key, pubKey, privKey) = clientKey.asString

    val response = create(data)

    store(
      s"${Configs.ENV},$uuid,$algo,$pubKey,$privKey,$key\n".getBytes(StandardCharsets.UTF_8),
      PATH_KEYS,
      response.status
    )

    (key, pubKey, privKey, (info, data, response))
  }

  def logOutput(info: String, data: String, resp: ResponseData[String]): Unit = {
    logger.info("Info: " + info)
    logger.info("Data: " + data)
    logger.info("Response: " + resp.body)
    printStatus(resp.status)
  }

}
