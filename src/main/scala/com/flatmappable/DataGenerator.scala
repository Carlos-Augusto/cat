package com.flatmappable

import java.util.UUID

import com.flatmappable.models.{ PayloadGenerator, SimpleProtocolImpl }
import com.flatmappable.util.WithJsonFormats
import com.typesafe.scalalogging.LazyLogging
import com.ubirch.crypto.PrivKey
import com.ubirch.protocol.Protocol

private class Total {
  @volatile private var added = 0
  def inc: Unit = added = added + 1
  def total: Int = added
}

case class SimpleDataGeneration(UUID: UUID, upp: String, hash: String)

class DataGenerator(total: Total, uuid: UUID, clientKey: PrivKey)
  extends WithJsonFormats with LazyLogging {

  val protocol = new SimpleProtocolImpl(uuid, clientKey)
  val payloadGenerator = new PayloadGenerator(uuid, protocol)

  def generate(maxMessages: Int, format: Protocol.Format)(dump: (UUID, String, String) => Unit) = {
    Iterator
      .continually(payloadGenerator.getOne(format, withNonce = true))
      .take(maxMessages)
      .foreach { case (_, upp, hash) =>
        dump(uuid, upp, hash)
      }
  }

  def toList(maxNumberOfMessages: Int, format: Protocol.Format) = {
    val buf = scala.collection.mutable.ListBuffer.empty[SimpleDataGeneration]
    generate(maxNumberOfMessages, format) { (uuid, upp, hash) =>
      buf += SimpleDataGeneration(uuid, upp, hash)
      total.inc
    }
    buf.toList
  }

  def single(data: String, format: Protocol.Format, withNonce: Boolean) = {
    total.inc
    payloadGenerator.fromString(data, format, withNonce)
  }

}

object DataGenerator {

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

