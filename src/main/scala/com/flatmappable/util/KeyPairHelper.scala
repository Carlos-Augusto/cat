package com.flatmappable.util

import java.util.Base64

import com.ubirch.crypto.utils.Curve
import com.ubirch.crypto.{ GeneratorKeyFactory, PrivKey }

object KeyPairHelper {

  def privateKey = GeneratorKeyFactory.getPrivKey(Curve.Ed25519)

  def createKeys = createKeysAsString(privateKey)

  def createKeysAsString(pk: PrivKey) = {
    val fullprivKey = Base64.getEncoder.encodeToString(pk.getPrivateKey.getEncoded)
    val privKey = Base64.getEncoder.encodeToString(pk.getRawPrivateKey.slice(0, 32))
    val pubKey = Base64.getEncoder.encodeToString(pk.getRawPublicKey.slice(0, 32))
    (fullprivKey, pubKey, privKey)
  }

}
