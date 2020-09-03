package com.flatmappable

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Paths, StandardOpenOption }
import java.util.UUID

import com.flatmappable.util.KeyPairHelper
import com.ubirch.crypto.PrivKey

object KeyRegistrationCat {

  def createPrivKey = KeyPairHelper.privateKey

  def extractKeys(privKey: PrivKey) = KeyPairHelper.createKeysAsString(privKey)

  def newRegistration(uuid: UUID) = {
    val (pubKey, privKey) = KeyPairHelper.createKeysAsString(createPrivKey)
    val response = KeyRegistration.register(uuid, pubKey, privKey)

    if (response._4.getStatusLine.getStatusCode <= 200 || response._4.getStatusLine.getStatusCode <= 200) {
      val keyLineToSave = s"$uuid,ECC_ED25519,$pubKey,$privKey\n".getBytes(StandardCharsets.UTF_8)
      Files.write(Paths.get(System.getProperty("user.home") + "/.cat/.keys"), keyLineToSave, StandardOpenOption.CREATE, StandardOpenOption.APPEND)
    }

    (pubKey, privKey, response)

  }

}
