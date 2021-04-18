package com.flatmappable

import java.nio.charset.StandardCharsets
import java.util.UUID

import com.flatmappable.util.KeyPairHelper.EnrichedPrivKey
import com.flatmappable.util._
import com.ubirch.crypto.PrivKey
import org.joda.time.DateTime
import org.json4s.jackson.JsonMethods._

case class KeyRegistration(keyCreationData: String, clientKey: PrivKey, responseData: ResponseData[String]) {
  def getKeyInfo: String = {
    s"""
      |Key Creation Data
      |-----------------
      |pub-key=${clientKey.getRawPublicKeyAsString}
      |priv-key=${clientKey.getRawPrivateKeyAsString}
      |priv-key-full=${clientKey.getPrivateKeyAsString}
      |""".stripMargin
  }
}

object KeyRegistration {

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

  def createKey(
      uuid: UUID,
      algo: Symbol = KeyPairHelper.ECC_ED25519,
      clientKey: PrivKey = KeyPairHelper.privateKeyEd25519,
      created: Long = now
  ): (PrivKey, String) = {
    val pubKey = clientKey.getRawPublicKeyAsString
    val info = compact(parse(pubKeyInfoData(uuid, algo, pubKey, created)))
    val signature = clientKey.sign(info.getBytes(StandardCharsets.UTF_8))
    val data = compact(parse(registrationData(info, toBase64AsString(signature))))
    val verification = clientKey.verify(info.getBytes, signature)

    if (!verification) throw new Exception("Key creation validation failed")

    (clientKey, data)

  }

  def newRegistration(uuid: UUID, algo: Symbol = KeyPairHelper.ECC_ED25519): KeyRegistration = {

    val clientKey = KeyPairHelper.getClientKey(algo)
    val (_, data) = createKey(uuid, algo = algo, clientKey = clientKey)

    val response = KeyCreation.create(data)

    doWhenOK(response.status) {
      Keys.insert(
        KeyRow(
          UUID.randomUUID(),
          Configs.ENV,
          uuid,
          algo = algo.toString(),
          privKey = clientKey.getPrivateKeyAsString,
          rawPrivKey = clientKey.getRawPrivateKeyAsString,
          rawPubKey = clientKey.getRawPublicKeyAsString,
          createdAt = new DateTime()
        )
      )
    }

    KeyRegistration(data, clientKey, response)

  }

}
