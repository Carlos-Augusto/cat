package com.flatmappable

import java.util.UUID

import com.ubirch.protocol.Protocol

object CatalinaHttp extends cask.MainRoutes {

  @cask.get("/")
  def hello() = {
    "This is Catalina."
  }

  @cask.post("/send/:uuid")
  def send(uuid: String, request: cask.Request) = {
    val identity = UUID.fromString(uuid)
    val body = request.bytes
    val privateKey = request.headers("x-pk").headOption.getOrElse(throw new Exception("No x-pk"))
    val pass = request.headers("x-pass").headOption.getOrElse(throw new Exception("No x-pass"))
    val (_, upp, hash) = DataGenerator.single(identity, body, privateKey, Protocol.Format.MSGPACK)
    val res = DataSending.send(identity, pass, DataGenerator.toBase64(hash), DataGenerator.toHex(upp))

    cask.Response(DataGenerator.toBase64(hash), res.status)
  }

  initialize()
}
