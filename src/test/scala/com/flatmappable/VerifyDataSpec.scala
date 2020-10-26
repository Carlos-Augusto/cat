package com.flatmappable

import com.flatmappable.util.Configs
import org.apache.http.util.EntityUtils
import org.scalatest.funsuite.AnyFunSuite

class VerifyDataSpec extends AnyFunSuite {

  test("VerifyData.verifyKeyRequest should be properly formed") {

    val url = "https://verify." + Configs.ENV + ".ubirch.com/api/upp"
    val hash = "this is a hash"
    val req = VerifyData.verifyKeyRequest(url, hash)

    assert(EntityUtils.toString(req.getEntity) == hash)

  }

}
