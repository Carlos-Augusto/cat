package com.flatmappable

import java.util.UUID

import com.ubirch.protocol.Protocol

object DataGeneratorCat {

  def generate(uuid: UUID, privateKey: String, format: Protocol.Format) = {
    val total = new Total
    val clientKey = KeyRegistration.getKey(privateKey)
    new DataGenerator(total, uuid, clientKey).toList(1, format)
  }

  def single(uuid: UUID, data: String, privateKey: String, format: Protocol.Format, withNonce: Boolean) = {
    val clientKey = KeyRegistration.getKey(privateKey)
    new DataGenerator(new Total, uuid, clientKey).single(data, format, withNonce)
  }

}

