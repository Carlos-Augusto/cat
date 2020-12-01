package com.flatmappable
package util

import com.ubirch.crypto.utils.Curve
import com.ubirch.crypto.{ GeneratorKeyFactory, PrivKey }

object KeyPairHelper {

  def privateKeyEd25519: PrivKey = GeneratorKeyFactory.getPrivKey(Curve.Ed25519)

  def privateKeyPRIME256V1: PrivKey = GeneratorKeyFactory.getPrivKey(Curve.PRIME256V1)

  def asString(pk: PrivKey): (String, String, String) = {
    (
      toBase64AsString(pk.getPrivateKey.getEncoded),
      toBase64AsString(pk.getRawPrivateKey),
      toBase64AsString(pk.getRawPublicKey)
    )
  }

}
