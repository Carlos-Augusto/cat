package com.flatmappable
import java.nio.charset.StandardCharsets
import java.util.UUID

import com.flatmappable.models.SimpleProtocolImpl
import com.ubirch.crypto.GeneratorKeyFactory
import com.ubirch.crypto.utils.Curve
import com.ubirch.protocol.Protocol
import com.ubirch.protocol.codec.UUIDUtil
import org.scalatest.funsuite.AnyFunSuite

import scala.util.{ Failure, Random, Success }

class DataGeneratorSpec extends AnyFunSuite {

  test("DataGenerator.generate should generate simple data generation") {
    val pk = GeneratorKeyFactory.getPrivKey(Curve.Ed25519)
    val pkAsString = toBase64AsString(pk.getRawPrivateKey)
    val uuid = UUID.randomUUID()
    val sdg = DataGenerator.single(uuid, 3.toString.getBytes(StandardCharsets.UTF_8), pkAsString, Protocol.Format.MSGPACK, withNonce = false)

    assert(sdg.isSuccess)
  }

  test("DataGenerator.simple should generate expected data from data as bytes") {
    val pk = GeneratorKeyFactory.getPrivKey(Curve.Ed25519)
    val pkAsString = toBase64AsString(pk.getRawPrivateKey)
    val uuid = UUID.randomUUID()
    val data = UUID.randomUUID()
    val dataAsBytes = UUIDUtil.uuidToBytes(data)
    val sdg = DataGenerator.single(uuid, dataAsBytes, pkAsString, Protocol.Format.MSGPACK, withNonce = false)

    assert(sdg.isSuccess)
    assert(sdg.get.protocolMessage != null)
    assert(sdg.get.upp.nonEmpty)
    assert(sdg.get.hash.nonEmpty)
  }

  test("DataGenerator.simple should generate expected data from data as string") {
    val pk = GeneratorKeyFactory.getPrivKey(Curve.Ed25519)
    val pkAsString = toBase64AsString(pk.getRawPrivateKey)
    val uuid = UUID.randomUUID()
    val data = Random.nextBytes(10)
    val res = DataGenerator.single(uuid, data, pkAsString, Protocol.Format.MSGPACK, withNonce = true)

    assert(res.isSuccess)
    assert(res.get.protocolMessage != null)
    assert(res.get.upp.nonEmpty)
    assert(res.get.hash.nonEmpty)

    val res2 = DataGenerator.single(uuid, data, pkAsString, Protocol.Format.MSGPACK, withNonce = false)

    assert(res2.isSuccess)
    assert(res2.get.protocolMessage != null)
    assert(res2.get.upp.nonEmpty)
    assert(res2.get.hash.nonEmpty)

    val res3 = DataGenerator.single(uuid, data, pkAsString, Protocol.Format.MSGPACK, withNonce = false)

    assert(res3.isSuccess)
    assert(res3.get.protocolMessage != null)
    assert(res3.get.upp.nonEmpty)
    assert(res3.get.hash.nonEmpty)

    assert(!(res.get.upp sameElements res2.get.upp))
    assert(res2.get.upp sameElements res3.get.upp)

    assert(!(res.get.hash sameElements res2.get.hash))
    assert(res2.get.hash sameElements res3.get.hash)

  }

  test("DataGenerator.buildMessage should generate data from bytes") {

    val pk = GeneratorKeyFactory.getPrivKey(Curve.Ed25519)
    val uuid = UUID.randomUUID()
    val data = UUID.randomUUID()
    val dataAsBytes = UUIDUtil.uuidToBytes(data)
    val protocol: Protocol = new SimpleProtocolImpl(uuid, pk)
    DataGenerator.buildMessage(uuid, protocol, Protocol.Format.MSGPACK, dataAsBytes, withNonce = false) match {
      case Failure(exception) => fail(exception)
      case Success(value) =>
        assert(value.protocolMessage != null)
        assert(value.upp.nonEmpty)
        assert(value.hash.nonEmpty)
    }

  }

  test("DataGenerator.buildMessage should generate data from string") {

    val pk = GeneratorKeyFactory.getPrivKey(Curve.Ed25519)
    val uuid = UUID.randomUUID()
    val data = UUID.randomUUID()
    val dataAsBytes = UUIDUtil.uuidToBytes(data)
    val protocol: Protocol = new SimpleProtocolImpl(uuid, pk)
    DataGenerator.buildMessage(uuid, protocol, Protocol.Format.MSGPACK, dataAsBytes, withNonce = false) match {
      case Failure(exception) => fail(exception)
      case Success(value) =>
        assert(value.protocolMessage != null)
        assert(value.upp.nonEmpty)
        assert(value.hash.nonEmpty)
    }

  }

  test("DataGenerator.buildMessageFromInt should generate data from int") {

    val pk = GeneratorKeyFactory.getPrivKey(Curve.Ed25519)
    val uuid = UUID.randomUUID()
    val data = Array(1.toByte)
    val protocol: Protocol = new SimpleProtocolImpl(uuid, pk)
    DataGenerator.buildMessage(uuid, protocol, Protocol.Format.MSGPACK, data, withNonce = false) match {
      case Failure(exception) => fail(exception)
      case Success(value) =>
        assert(value.protocolMessage != null)
        assert(value.upp.nonEmpty)
        assert(value.hash.nonEmpty)
    }

  }

  test("DataGenerator.buildMessageFromString should generate data from string") {

    val pk = GeneratorKeyFactory.getPrivKey(Curve.Ed25519)
    val uuid = UUID.randomUUID()
    val data = "1".getBytes()
    val protocol: Protocol = new SimpleProtocolImpl(uuid, pk)
    DataGenerator.buildMessage(uuid, protocol, Protocol.Format.MSGPACK, data, withNonce = false) match {
      case Failure(exception) => fail(exception)
      case Success(value) =>
        assert(value.protocolMessage != null)
        assert(value.upp.nonEmpty)
        assert(value.hash.nonEmpty)
    }

  }

  test("DataGenerator.buildMessageFromString should generate data from json with special characters") {

    val pk = GeneratorKeyFactory.getPrivKey(Curve.Ed25519)
    val uuid = UUID.randomUUID()
    val data = """{"data":"let's do it, ich möchte, nämlich"}""".stripMargin.getBytes(StandardCharsets.UTF_8)
    val protocol: Protocol = new SimpleProtocolImpl(uuid, pk)
    DataGenerator.buildMessage(uuid, protocol, Protocol.Format.MSGPACK, data, withNonce = false) match {
      case Failure(exception) => fail(exception)
      case Success(value) =>
        assert(value.protocolMessage != null)
        assert(value.upp.nonEmpty)
        assert(value.hash.nonEmpty)
        assert(value.hashAsBase64 == """Mh2zkLvnPm9SnR/7PhSCQfQnko6erqs6CsLRqVxVKBT0HsGaaIxGHFOaI7TV5VARNAyulzlO+KI6+hRUZIdvhw==""")
    }

  }

}
