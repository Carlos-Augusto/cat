package com.flatmappable
package util

import com.ubirch.crypto.utils.Curve
import com.ubirch.crypto.{ GeneratorKeyFactory, PrivKey }

object KeyPairHelper {

  def privateKey = GeneratorKeyFactory.getPrivKey(Curve.Ed25519)

  def createKeys = createKeysAsString(privateKey)

  def createKeysAsString(pk: PrivKey) = {
    val fullprivKey = toBase64AsString(pk.getPrivateKey.getEncoded)
    val privKey = toBase64AsString(pk.getRawPrivateKey.slice(0, 32))
    val pubKey = toBase64AsString(pk.getRawPublicKey.slice(0, 32))
    (fullprivKey, pubKey, privKey)
  }

}
