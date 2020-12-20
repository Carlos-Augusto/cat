package com.flatmappable
package util

import com.ubirch.crypto.utils.Curve
import com.ubirch.crypto.{ GeneratorKeyFactory, PrivKey }

object KeyPairHelper {

  def privateKeyEd25519(key: Array[Byte]): PrivKey = GeneratorKeyFactory.getPrivKey(key, Curve.Ed25519)
  def privateKeyEd25519: PrivKey = GeneratorKeyFactory.getPrivKey(Curve.Ed25519)

  def privateKeyPRIME256V1(key: Array[Byte]): PrivKey = GeneratorKeyFactory.getPrivKey(key, Curve.PRIME256V1)
  def privateKeyPRIME256V1: PrivKey = GeneratorKeyFactory.getPrivKey(Curve.PRIME256V1)

  implicit class EnrichedPrivKey(pk: PrivKey) {

    def getPrivateKeyAsString: String = toBase64AsString(pk.getPrivateKey.getEncoded)
    def getRawPublicKeyAsString: String = toBase64AsString(pk.getRawPublicKey)
    def getRawPrivateKeyAsString: String = toBase64AsString(pk.getRawPrivateKey)

    def asString: (String, String, String) = {
      (
        getPrivateKeyAsString,
        getRawPrivateKeyAsString,
        getRawPublicKeyAsString
      )
    }
  }

}
