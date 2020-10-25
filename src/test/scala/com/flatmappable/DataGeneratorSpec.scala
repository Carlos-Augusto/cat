package com.flatmappable
import java.util.UUID

import com.flatmappable.models.SimpleProtocolImpl
import com.ubirch.crypto.GeneratorKeyFactory
import com.ubirch.crypto.utils.Curve
import com.ubirch.protocol.Protocol
import com.ubirch.protocol.codec.UUIDUtil
import org.scalatest.funsuite.AnyFunSuite

class DataGeneratorSpec extends AnyFunSuite {

  test("DataGenerator.generate should generate simple data generation") {
    val pk = GeneratorKeyFactory.getPrivKey(Curve.Ed25519)
    val pkAsString = toBase64AsString(pk.getRawPrivateKey)
    val uuid = UUID.randomUUID()
    val sdg = DataGenerator.generate(uuid, pkAsString, Protocol.Format.MSGPACK, 3)

    assert(sdg.nonEmpty)
    assert(sdg.size == 3)
  }

  test("DataGenerator.simple should generate expected data from data as bytes") {
    val pk = GeneratorKeyFactory.getPrivKey(Curve.Ed25519)
    val pkAsString = toBase64AsString(pk.getRawPrivateKey)
    val uuid = UUID.randomUUID()
    val data = UUID.randomUUID()
    val dataAsBytes = UUIDUtil.uuidToBytes(data)
    val (pm, upp, hash) = DataGenerator.single(uuid, dataAsBytes, pkAsString, Protocol.Format.MSGPACK)

    assert(pm != null)
    assert(upp.nonEmpty)
    assert(hash.nonEmpty)
  }

  test("DataGenerator.simple should generate expected data from data as string") {
    val pk = GeneratorKeyFactory.getPrivKey(Curve.Ed25519)
    val pkAsString = toBase64AsString(pk.getRawPrivateKey)
    val uuid = UUID.randomUUID()
    val data = UUID.randomUUID()
    val (pm, upp, hash) = DataGenerator.single(uuid, data.toString, pkAsString, Protocol.Format.MSGPACK, withNonce = true)

    assert(pm != null)
    assert(upp.nonEmpty)
    assert(hash.nonEmpty)

    val (pm2, upp2, hash2) = DataGenerator.single(uuid, data.toString, pkAsString, Protocol.Format.MSGPACK, withNonce = false)

    assert(pm2 != null)
    assert(upp2.nonEmpty)
    assert(hash2.nonEmpty)

    val (pm3, upp3, hash3) = DataGenerator.single(uuid, data.toString, pkAsString, Protocol.Format.MSGPACK, withNonce = false)

    assert(pm3 != null)
    assert(upp3.nonEmpty)
    assert(hash3.nonEmpty)

    assert(upp != upp2)
    assert(upp2 == upp3)

    assert(hash != hash2)
    assert(hash2 == hash3)

  }

  test("DataGenerator.buildMessage should generate data from bytes") {

    val pk = GeneratorKeyFactory.getPrivKey(Curve.Ed25519)
    val uuid = UUID.randomUUID()
    val data = UUID.randomUUID()
    val dataAsBytes = UUIDUtil.uuidToBytes(data)
    val protocol = new SimpleProtocolImpl(uuid, pk)
    val (pm, upp, hash) = DataGenerator.buildMessage(uuid, protocol, Protocol.Format.MSGPACK, dataAsBytes)

    assert(pm != null)
    assert(upp.nonEmpty)
    assert(hash.nonEmpty)

  }

  test("DataGenerator.buildMessage should generate data from string") {

    val pk = GeneratorKeyFactory.getPrivKey(Curve.Ed25519)
    val uuid = UUID.randomUUID()
    val data = UUID.randomUUID()
    val protocol = new SimpleProtocolImpl(uuid, pk)
    val (pm, upp, hash) = DataGenerator.buildMessage(uuid, protocol, Protocol.Format.MSGPACK, data.toString, withNonce = false)

    assert(pm != null)
    assert(upp.nonEmpty)
    assert(hash.nonEmpty)

  }

  test("DataGenerator.buildMessageFromInt should generate data from int") {

    val pk = GeneratorKeyFactory.getPrivKey(Curve.Ed25519)
    val uuid = UUID.randomUUID()
    val data = 1
    val protocol = new SimpleProtocolImpl(uuid, pk)
    val (pm, upp, hash) = DataGenerator.buildMessageFromInt(uuid, protocol, Protocol.Format.MSGPACK, data, withNonce = false)

    assert(pm != null)
    assert(upp.nonEmpty)
    assert(hash.nonEmpty)

  }

  test("DataGenerator.buildMessageFromString should generate data from string") {

    val pk = GeneratorKeyFactory.getPrivKey(Curve.Ed25519)
    val uuid = UUID.randomUUID()
    val data = "1"
    val protocol = new SimpleProtocolImpl(uuid, pk)
    val (pm, upp, hash) = DataGenerator.buildMessageFromString(uuid, protocol, Protocol.Format.MSGPACK, data, withNonce = false)

    assert(pm != null)
    assert(upp.nonEmpty)
    assert(hash.nonEmpty)

  }

}
