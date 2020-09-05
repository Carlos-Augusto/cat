package com.flatmappable

import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.{ Base64, UUID }

import com.flatmappable.models.SimpleProtocolImpl
import com.flatmappable.util.WithJsonFormats
import com.typesafe.scalalogging.LazyLogging
import com.ubirch.crypto.PrivKey
import com.ubirch.protocol.{ Protocol, ProtocolMessage }
import org.apache.commons.codec.binary.Hex

private class Total {
  @volatile private var added = 0
  def inc(): Unit = added = added + 1
  def total: Int = added
}

case class SimpleDataGeneration(UUID: UUID, upp: String, hash: String)

class DataGenerator(total: Total, uuid: UUID, clientKey: PrivKey)
  extends WithJsonFormats with LazyLogging {

  val protocol = new SimpleProtocolImpl(uuid, clientKey)

  def generate(maxMessages: Int, format: Protocol.Format)(dump: (UUID, String, String) => Unit) = {
    Iterator
      .continually {
        DataGenerator.buildMessageFromInt(uuid, protocol, format, (Math.random * 10 + 10).toInt, withNonce = true)
      }
      .take(maxMessages)
      .foreach { case (_, upp, hash) =>
        dump(uuid, upp, hash)
      }
  }

  def toList(maxNumberOfMessages: Int, format: Protocol.Format) = {
    val buf = scala.collection.mutable.ListBuffer.empty[SimpleDataGeneration]
    generate(maxNumberOfMessages, format) { (uuid, upp, hash) =>
      buf += SimpleDataGeneration(uuid, upp, hash)
      total.inc()
    }
    buf.toList
  }

  def single(data: String, format: Protocol.Format, withNonce: Boolean) = {
    total.inc()
    DataGenerator.buildMessageFromString(uuid, protocol, format, data, withNonce)
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

  def toBase64(data: Array[Byte]): String = Base64.getEncoder.encodeToString(data)

  def toHex(data: Array[Byte]): String = Hex.encodeHexString(data)

  def toBytesFromHex(data: String): Array[Byte] = Hex.decodeHex(data)

  def buildMessage(clientUUID: UUID, protocol: SimpleProtocolImpl, format: Protocol.Format, data: String, withNonce: Boolean): (ProtocolMessage, Array[Byte], Array[Byte]) = {
    val df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    val ts = System.currentTimeMillis
    val message = if (withNonce) data + "," + df.format(ts) + "," + clientUUID.toString else data
    val hash = MessageDigest.getInstance("SHA-512").digest(message.getBytes)
    val pm = new ProtocolMessage(ProtocolMessage.SIGNED, clientUUID, 0x00, hash)
    val upp = protocol.encodeSign(pm, format)
    (pm, upp, hash)
  }

  def buildMessageFromInt(clientUUID: UUID, protocol: SimpleProtocolImpl, format: Protocol.Format, temp: Int, withNonce: Boolean): (ProtocolMessage, String, String) = {
    val (pm, upp, hash) = buildMessage(clientUUID, protocol, format, temp.toString, withNonce)
    (pm, toHex(upp), toBase64(hash))
  }

  def buildMessageFromString(clientUUID: UUID, protocol: SimpleProtocolImpl, format: Protocol.Format, data: String, withNonce: Boolean): (ProtocolMessage, String, String) = {
    val (pm, upp, hash) = buildMessage(clientUUID, protocol, format, data, withNonce)
    (pm, toHex(upp), toBase64(hash))
  }

}
