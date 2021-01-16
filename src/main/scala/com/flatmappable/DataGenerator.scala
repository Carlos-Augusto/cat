package com.flatmappable

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.UUID

import com.flatmappable.models.SimpleProtocolImpl
import com.flatmappable.util.KeyPairHelper
import com.ubirch.protocol.codec.UUIDUtil
import com.ubirch.protocol.{ Protocol, ProtocolMessage }

import scala.util.Try

case class SimpleDataGeneration(UUID: UUID, protocolMessage: ProtocolMessage, upp: Array[Byte], hash: Array[Byte]) {
  def uppAsHex: String = toHex(upp)
  def hashAsBase64: String = toBase64AsString(hash)
}

object DataGenerator {

  def buildMessage(uuid: UUID, data: Array[Byte], privateKey: String, format: Protocol.Format, withNonce: Boolean): Try[SimpleDataGeneration] = {
    for {
      clientKey <- Try(KeyPairHelper.privateKeyEd25519(privateKey))
      protocol: Protocol <- Try(new SimpleProtocolImpl(uuid, clientKey))
      msg <- buildMessage(uuid, data, protocol, format, withNonce)
    } yield msg
  }

  //upp usually is in hex and hash in base64
  def buildMessage(uuid: UUID, data: Array[Byte], protocol: Protocol, format: Protocol.Format, withNonce: Boolean): Try[SimpleDataGeneration] = Try {
    val nowAsBytes = defaultDataFormat.format(System.currentTimeMillis).getBytes(StandardCharsets.UTF_8)
    val uuidAsBytes = UUIDUtil.uuidToBytes(uuid)
    val sep = ",".getBytes(StandardCharsets.UTF_8)
    val message = if (withNonce) Array.concat(data, sep, nowAsBytes, sep, uuidAsBytes) else data
    val hash = MessageDigest.getInstance("SHA-512").digest(message)
    val pm = new ProtocolMessage(ProtocolMessage.SIGNED, uuid, 0x00, hash)
    val upp = protocol.encodeSign(pm, format)
    SimpleDataGeneration(uuid, pm, upp, hash)
  }

}
