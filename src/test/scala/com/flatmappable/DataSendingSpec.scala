package com.flatmappable

import java.util.UUID

import com.ubirch.protocol.codec.UUIDUtil
import org.apache.http.util.EntityUtils
import org.scalatest.funsuite.AnyFunSuite

class DataSendingSpec extends AnyFunSuite {

  test("DataSending.sendKeyRequest should be properly formed") {
    val uuid = UUID.randomUUID()
    val pass = "123"
    val data = UUIDUtil.uuidToBytes(uuid)

    val req = DataSending.sendKeyRequest(uuid, pass, data)
    val headers = req.getAllHeaders.toList

    assert(headers.size == 4)
    assert(headers.exists(_.getName == DataSending.CONTENT_TYPE))
    assert(headers.exists(_.getName == DataSending.X_UBIRCH_CREDENTIAL))
    assert(headers.exists(_.getName == DataSending.X_UBIRCH_AUTH_TYPE))
    assert(headers.exists(_.getName == DataSending.X_UBIRCH_HARDWARE_ID))
    assert(headers.find(_.getName == DataSending.CONTENT_TYPE).exists(_.getValue == "application/octet-stream"))
    assert(headers.find(_.getName == DataSending.X_UBIRCH_CREDENTIAL).exists(_.getValue == pass))
    assert(headers.find(_.getName == DataSending.X_UBIRCH_AUTH_TYPE).exists(_.getValue == "ubirch"))
    assert(headers.find(_.getName == DataSending.X_UBIRCH_HARDWARE_ID).exists(_.getValue == uuid.toString))
    assert(EntityUtils.toByteArray(req.getEntity) sameElements data)

  }

}
