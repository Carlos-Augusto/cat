package com.flatmappable

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Paths, StandardOpenOption }
import java.security.{ InvalidKeyException, NoSuchAlgorithmException }
import java.text.SimpleDateFormat
import java.util.{ Base64, TimeZone, UUID }

import com.flatmappable.util.{ Configs, HttpHelpers, KeyPairHelper, WithJsonFormats }
import com.typesafe.scalalogging.LazyLogging
import com.ubirch.crypto.utils.Curve
import com.ubirch.crypto.{ GeneratorKeyFactory, PrivKey, PubKey }
import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import org.json4s.jackson.JsonMethods._

object KeyRegistration extends WithJsonFormats with LazyLogging {

  val client: HttpClient = HttpClients.createMinimal()

  val df = {
    val _df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    _df.setTimeZone(TimeZone.getTimeZone("UTC"))
    _df
  }

  def pubKeyInfoData(clientUUID: UUID, df: SimpleDateFormat, sk: String) = {
    val now = System.currentTimeMillis()
    s"""
      |{
      |   "algorithm": "ECC_ED25519",
      |   "created": "${df.format(now)}",
      |   "hwDeviceId": "${clientUUID.toString}",
      |   "pubKey": "$sk",
      |   "pubKeyId": "$sk",
      |   "validNotAfter": "${df.format(now + 31557600000L)}",
      |   "validNotBefore": "${df.format(now)}"
      |}
    """.stripMargin
  }

  def registrationData(pubKeyInfoData: String, signature: String) = {
    s"""
      |{
      |   "pubKeyInfo": $pubKeyInfoData,
      |   "signature": "$signature"
      |}
    """.stripMargin
  }

  def registerKeyRequest(body: String) = {
    val regRequest = new HttpPost("https://key." + Configs.ENV + ".ubirch.com/api/keyService/v1/pubkey")
    regRequest.setHeader("Content-Type", "application/json")
    regRequest.setEntity(new StringEntity(body))
    regRequest
  }

  def getKey(privateKey: String) = {
    val clientKeyBytes = Base64.getDecoder.decode(privateKey)
    createClientKey(clientKeyBytes)
  }

  def register(UUID: UUID, publicKey: String, privateKey: String) = {

    val clientKey = getKey(privateKey)

    val info = compact(parse(pubKeyInfoData(UUID, df, publicKey)))
    val signature = clientKey.sign(info.getBytes(StandardCharsets.UTF_8))
    val data = compact(parse(registrationData(info, Base64.getEncoder.encodeToString(signature))))

    val verification = clientKey.verify(info.getBytes, signature)
    val resp = client.execute(registerKeyRequest(data))
    val body = HttpHelpers.readEntity(resp)

    (info, data, verification, resp, body)
  }

  def logOutput(info: String, data: String, verification: Boolean, resp: HttpResponse, body: String) = {
    logger.info("Info: " + info)
    logger.info("Data: " + data)
    logger.info("Verification: " + verification.toString)
    logger.info("Response: " + body)
    HttpHelpers.printStatus(resp.getStatusLine.getStatusCode)
  }

  def createClientKey(clientKeyBytes: Array[Byte]): PrivKey = {
    try
      GeneratorKeyFactory.getPrivKey(clientKeyBytes, Curve.Ed25519)
    catch {
      case e @ (_: NoSuchAlgorithmException | _: InvalidKeyException) =>
        logger.error("Missing or broken CLIENT_KEY (base64)")
        throw e
    }
  }

  def newRegistration(uuid: UUID) = {
    val (key, pubKey, privKey) = KeyPairHelper.createKeysAsString(KeyPairHelper.privateKey)
    val response = KeyRegistration.register(uuid, pubKey, privKey)

    if (response._4.getStatusLine.getStatusCode <= 200 || response._4.getStatusLine.getStatusCode <= 200) {
      val keyLineToSave = s"${Configs.ENV},$uuid,ECC_ED25519,$pubKey,$privKey\n".getBytes(StandardCharsets.UTF_8)
      Files.write(Paths.get(System.getProperty("user.home") + "/.cat/.keys"), keyLineToSave, StandardOpenOption.CREATE, StandardOpenOption.APPEND)
    }

    (key, pubKey, privKey, response)

  }

}
