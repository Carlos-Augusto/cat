package com.flatmappable.models

import java.util.UUID

import com.ubirch.protocol.Protocol

class PayloadGenerator(clientUUID: UUID, protocol: SimpleProtocolImpl) {

  def getOne(format: Protocol.Format, withNonce: Boolean) =
    AbstractUbirchClient.buildMessageFromInt(clientUUID, protocol, format, (Math.random * 10 + 10).toInt, withNonce)

  def fromString(data: String, format: Protocol.Format, withNonce: Boolean) =
    AbstractUbirchClient.buildMessageFromString(clientUUID, protocol, format, data, withNonce)

}
