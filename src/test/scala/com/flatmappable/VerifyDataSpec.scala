package com.flatmappable

import com.flatmappable.util.Configs
import org.apache.http.util.EntityUtils
import org.scalatest.funsuite.AnyFunSuite

class VerifyDataSpec extends AnyFunSuite {

  test("VerifyData.verifyKeyRequest should be properly formed: Simple") {

    val url = Configs.SIMPLE_VERIFICATION_URL
    val hash = "this is a hash"
    val req = VerifyData.verifyKeyRequest(url, hash)

    assert(req.getURI.toString == "https://verify.dev.ubirch.com/api/upp")
    assert(EntityUtils.toString(req.getEntity) == hash)

  }

  test("VerifyData.verifyKeyRequest should be properly formed: Initial") {

    val url = Configs.INITIAL_VERIFICATION_URL
    val hash = "this is a hash"
    val req = VerifyData.verifyKeyRequest(url, hash)

    assert(req.getURI.toString == "https://verify.dev.ubirch.com/api/upp/verify")
    assert(EntityUtils.toString(req.getEntity) == hash)

  }

  test("VerifyData.verifyKeyRequest should be properly formed: Upper") {

    val url = Configs.UPPER_VERIFICATION_URL
    val hash = "this is a hash"
    val req = VerifyData.verifyKeyRequest(url, hash)

    assert(req.getURI.toString == "https://verify.dev.ubirch.com/api/upp/verify/anchor")
    assert(EntityUtils.toString(req.getEntity) == hash)

  }

  test("VerifyData.verifyKeyRequest should be properly formed: Record") {

    val url = Configs.FULL_VERIFICATION_URL
    val hash = "this is a hash"
    val req = VerifyData.verifyKeyRequest(url, hash)

    assert(req.getURI.toString == "https://verify.dev.ubirch.com/api/upp/verify/record")
    assert(EntityUtils.toString(req.getEntity) == hash)

  }

}
