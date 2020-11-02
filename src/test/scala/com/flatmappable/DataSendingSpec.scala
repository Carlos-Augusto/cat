package com.flatmappable

import java.util.UUID

import com.ubirch.protocol.codec.UUIDUtil
import org.apache.http.util.EntityUtils
import org.scalatest.funsuite.AnyFunSuite

class DataSendingSpec extends AnyFunSuite {

  test("DataSending.sendKeyRequest should be properly formed") {
    val uuid = UUID.randomUUID()
    val pass = "123"
    val extraHeaders = Map("x-h" -> Seq("uno", "dos"), "x-y" -> Seq("tres"))
    val data = UUIDUtil.uuidToBytes(uuid)

    val req = DataSending.sendKeyRequest(uuid, pass, data, extraHeaders)
    val headers = req.getAllHeaders.toList

    assert(headers.size == 7)
    assert(headers.find(_.getName == CONTENT_TYPE).exists(_.getValue == "application/octet-stream"))
    assert(headers.find(_.getName == X_UBIRCH_CREDENTIAL).exists(_.getValue == pass))
    assert(headers.find(_.getName == X_UBIRCH_AUTH_TYPE).exists(_.getValue == "ubirch"))
    assert(headers.find(_.getName == X_UBIRCH_HARDWARE_ID).exists(_.getValue == uuid.toString))

    assert(headers.filter(_.getName == "x-h").exists(_.getValue == "uno"))
    assert(headers.filter(_.getName == "x-h").exists(_.getValue == "dos"))
    assert(headers.filter(_.getName == "x-y").exists(_.getValue == "tres"))

    assert(EntityUtils.toByteArray(req.getEntity) sameElements data)

  }

}
