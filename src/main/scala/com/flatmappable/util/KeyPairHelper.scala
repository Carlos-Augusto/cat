package com.flatmappable
package util

import java.util.Base64

import com.ubirch.crypto.utils.Curve
import com.ubirch.crypto.{ GeneratorKeyFactory, PrivKey }

object KeyPairHelper {

  val ECC_ECDSA: Symbol = Symbol("ECC_ECDSA")
  val ECC_ED25519: Symbol = Symbol("ECC_ED25519")

  def getClientKey(algo: Symbol): PrivKey = algo match {
    case ECC_ED25519 => KeyPairHelper.privateKeyEd25519
    case ECC_ECDSA => KeyPairHelper.privateKeyPRIME256V1
  }

  def privateKeyEd25519(key: Array[Byte]): PrivKey = GeneratorKeyFactory.getPrivKey(key, Curve.Ed25519)
  def privateKeyEd25519(key: String): PrivKey = privateKeyEd25519(Base64.getDecoder.decode(key))
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
