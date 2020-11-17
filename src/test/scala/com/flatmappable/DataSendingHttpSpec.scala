package com.flatmappable

import java.util.UUID

import com.flatmappable.util.ResponseData
import io.undertow.Undertow
import org.scalatest.funsuite.AnyFunSuite
import requests.RequestFailedException

import scala.util.Try

class DataSendingHttpSpec extends AnyFunSuite {

  def withServer[T](example: cask.main.Main)(f: String => T): T = {
    val server = Undertow.builder
      .addHttpListener(8081, CatalinaHttp.host)
      .setHandler(example.defaultHandler)
      .build
    server.start()
    val res =
      try f("http://localhost:8081")
      finally server.stop()
    res

  }

  test("CatalinaHttp.configs should exist") {

    withServer(CatalinaHttp) { host =>

      val res = requests.get(s"$host/configs").statusCode
      assert(res == 200)

    }

  }

  test("CatalinaHttp should exist") {

    withServer(CatalinaHttp) { host =>

      val res = requests.get(s"$host").statusCode
      assert(res == 200)

    }

  }

  class Cat() extends CatalinaHttpBase {
    override def sendData(uuid: UUID, password: String, hash: Array[Byte], upp: Array[Byte], extraHeaders: Map[String, Seq[String]]): ResponseData[Array[Byte]] = {
      ResponseData(200, Array.empty, hash)
    }
  }

  test("CatalinaHttp.send should fail when no body is sent") {

    withServer(new Cat()) { host =>

      val uuid = UUID.randomUUID()

      val res = Try(requests.post(s"$host/send/$uuid"))
        .recover { case e: RequestFailedException => e.response }
        .get

      assert(res.statusCode == 400)
      assert(res.text() == """{"status":400,"message":"BadRequest","data":"Empty body"}""")

    }

  }

}
