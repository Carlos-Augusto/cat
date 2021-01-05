package com.flatmappable

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.UUID

import com.flatmappable.models.SimpleProtocolImpl
import com.flatmappable.util.{ KeyPairHelper, Logging }
import com.ubirch.crypto.PrivKey
import com.ubirch.protocol.{ Protocol, ProtocolMessage }

case class SimpleDataGeneration(UUID: UUID, upp: String, hash: String)

class DataGenerator(uuid: UUID, clientKey: PrivKey) extends Logging {

  val protocol: Protocol = new SimpleProtocolImpl(uuid, clientKey)

  def generate(maxMessages: Int, format: Protocol.Format)(dump: (UUID, String, String) => Unit): Unit = {
    Iterator
      .continually {
        DataGenerator.buildMessageFromInt(uuid, protocol, format, (Math.random * 10 + 10).toInt, withNonce = true)
      }
      .take(maxMessages)
      .foreach { case (_, upp, hash) =>
        dump(uuid, upp, hash)
      }
  }

  def toList(maxNumberOfMessages: Int, format: Protocol.Format): List[SimpleDataGeneration] = {
    val buf = scala.collection.mutable.ListBuffer.empty[SimpleDataGeneration]
    generate(maxNumberOfMessages, format) { (uuid, upp, hash) =>
      buf += SimpleDataGeneration(uuid, upp, hash)
    }
    buf.toList
  }

  def single(data: String, format: Protocol.Format, withNonce: Boolean): (ProtocolMessage, String, String) = {
    DataGenerator.buildMessageFromString(uuid, protocol, format, data, withNonce)
  }

  def single(data: Array[Byte], format: Protocol.Format): (ProtocolMessage, Array[Byte], Array[Byte]) = {
    DataGenerator.buildMessage(uuid, protocol, format, data)
  }

}

object DataGenerator {

  def generate(uuid: UUID, privateKey: String, format: Protocol.Format, maxNumberOfMessages: Int = 1): List[SimpleDataGeneration] = {
    val clientKey = KeyPairHelper.privateKeyEd25519(privateKey)
    new DataGenerator(uuid, clientKey).toList(maxNumberOfMessages, format)
  }

  def single(uuid: UUID, data: String, privateKey: String, format: Protocol.Format, withNonce: Boolean): (ProtocolMessage, String, String) = {
    val clientKey = KeyPairHelper.privateKeyEd25519(privateKey)
    new DataGenerator(uuid, clientKey).single(data, format, withNonce)
  }

  def single(uuid: UUID, data: Array[Byte], privateKey: String, format: Protocol.Format): (ProtocolMessage, Array[Byte], Array[Byte]) = {
    val clientKey = KeyPairHelper.privateKeyEd25519(privateKey)
    new DataGenerator(uuid, clientKey).single(data, format)
  }

  def buildMessage(clientUUID: UUID, protocol: Protocol, format: Protocol.Format, data: Array[Byte]): (ProtocolMessage, Array[Byte], Array[Byte]) = {
    val hash = MessageDigest.getInstance("SHA-512").digest(data)
    val pm = new ProtocolMessage(ProtocolMessage.SIGNED, clientUUID, 0x00, hash)
    val upp = protocol.encodeSign(pm, format)
    (pm, upp, hash)
  }

  def buildMessage(clientUUID: UUID, protocol: Protocol, format: Protocol.Format, data: String, withNonce: Boolean): (ProtocolMessage, Array[Byte], Array[Byte]) = {
    val message = if (withNonce) data + "," + defaultDataFormat.format(System.currentTimeMillis) + "," + clientUUID.toString else data
    buildMessage(clientUUID, protocol, format, message.getBytes(StandardCharsets.UTF_8))
  }

  def buildMessageFromInt(clientUUID: UUID, protocol: Protocol, format: Protocol.Format, temp: Int, withNonce: Boolean): (ProtocolMessage, String, String) = {
    val (pm, upp, hash) = buildMessage(clientUUID, protocol, format, temp.toString, withNonce)
    (pm, toHex(upp), toBase64AsString(hash))
  }

  def buildMessageFromString(clientUUID: UUID, protocol: Protocol, format: Protocol.Format, data: String, withNonce: Boolean): (ProtocolMessage, String, String) = {
    val (pm, upp, hash) = buildMessage(clientUUID, protocol, format, data, withNonce)
    (pm, toHex(upp), toBase64AsString(hash))
  }

}
