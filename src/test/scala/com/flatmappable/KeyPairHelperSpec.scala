package com.flatmappable

import java.util
import java.util.Base64

import com.flatmappable.util.KeyPairHelper
import com.flatmappable.util.KeyPairHelper.EnrichedPrivKey
import com.ubirch.crypto.GeneratorKeyFactory
import com.ubirch.crypto.utils.Curve
import org.scalatest.funsuite.AnyFunSuite

class KeyPairHelperSpec extends AnyFunSuite {

  test("KeyPairHelper.createKeysAsString Ed25519") {

    val k = KeyPairHelper.privateKeyEd25519
    val (a, b, c) = k.asString
    val aa = Base64.getDecoder.decode(a)
    val bb = Base64.getDecoder.decode(b)
    val cc = Base64.getDecoder.decode(c)

    assert(a == k.getPrivateKeyAsString)
    assert(b == k.getRawPrivateKeyAsString)
    assert(c == k.getRawPublicKeyAsString)

    assert(util.Arrays.equals(aa, k.getPrivateKey.getEncoded))
    assert(util.Arrays.equals(bb, k.getRawPrivateKey))
    assert(util.Arrays.equals(cc, k.getRawPublicKey))

    assert(aa.size == 83)
    assert(bb.size == 32)
    assert(cc.size == 32)

    val kk = GeneratorKeyFactory.getPrivKey(bb, Curve.Ed25519)
    val (_, bbb, ccc) = kk.asString

    assert(bbb == b)
    assert(ccc == c)

  }

  test("KeyPairHelper.createKeysAsString PRIME256V1") {
    val k = KeyPairHelper.privateKeyPRIME256V1
    val (a, b, c) = k.asString
    val aa = Base64.getDecoder.decode(a)
    val bb = Base64.getDecoder.decode(b)
    val cc = Base64.getDecoder.decode(c)

    assert(a == k.getPrivateKeyAsString)
    assert(b == k.getRawPrivateKeyAsString)
    assert(c == k.getRawPublicKeyAsString)

    assert(util.Arrays.equals(aa, k.getPrivateKey.getEncoded))
    assert(util.Arrays.equals(bb, k.getRawPrivateKey))
    assert(util.Arrays.equals(cc, k.getRawPublicKey))

    assert(aa.size == 150)
    assert(bb.size == 32 || bb.size == 33) // for when there is padding
    assert(cc.size == 64)

    val kk = GeneratorKeyFactory.getPrivKey(bb, Curve.PRIME256V1)
    val (_, bbb, ccc) = kk.asString

    assert(bbb == b)
    assert(ccc == c)

  }

}
