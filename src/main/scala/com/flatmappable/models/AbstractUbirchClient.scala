package com.flatmappable.models

import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.{ Base64, UUID }

import com.typesafe.scalalogging.LazyLogging
import com.ubirch.protocol.{ Protocol, ProtocolMessage }
import org.apache.commons.codec.binary.Hex

object AbstractUbirchClient extends LazyLogging {

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
    (pm, AbstractUbirchClient.toHex(upp), AbstractUbirchClient.toBase64(hash))
  }

  def buildMessageFromString(clientUUID: UUID, protocol: SimpleProtocolImpl, format: Protocol.Format, data: String, withNonce: Boolean): (ProtocolMessage, String, String) = {
    val (pm, upp, hash) = buildMessage(clientUUID, protocol, format, data, withNonce)
    (pm, AbstractUbirchClient.toHex(upp), AbstractUbirchClient.toBase64(hash))
  }

}
