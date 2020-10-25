package com.flatmappable

import java.util.UUID

import org.apache.http.util.EntityUtils
import org.json4s.jackson.JsonMethods._
import org.scalatest.funsuite.AnyFunSuite

class KeyRegistrationSpec extends AnyFunSuite {

  test("KeyRegistration.pubKeyInfoData should generate proper key data") {
    val uuid = UUID.fromString("387e9ac2-b1bc-4bdb-92b8-f3a279daa9c6")
    val sk = "this is a key"
    val created = 1603644866465L
    val ko = compact(parse(KeyRegistration.pubKeyInfoData(uuid, defaultDataFormat, sk, created)))
    val expected = """{"algorithm":"ECC_ED25519","created":"2020-10-25T16:54:26.465Z","hwDeviceId":"387e9ac2-b1bc-4bdb-92b8-f3a279daa9c6","pubKey":"this is a key","pubKeyId":"this is a key","validNotAfter":"2021-10-25T22:54:26.465Z","validNotBefore":"2020-10-25T16:54:26.465Z"}""".stripMargin

    assert(expected == ko)

  }

  test("KeyRegistration.registerKeyRequest should be properly formed") {

    val data = "123"
    val req = KeyRegistration.registerKeyRequest(data)
    val headers = req.getAllHeaders.toList

    assert(headers.find(_.getName == CONTENT_TYPE).exists(_.getValue == "application/json"))
    assert(EntityUtils.toString(req.getEntity) == data)

  }

}
