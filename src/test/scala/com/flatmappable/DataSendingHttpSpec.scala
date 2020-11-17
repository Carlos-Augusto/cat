package com.flatmappable

import java.util.UUID

import io.undertow.Undertow
import org.scalatest.funsuite.AnyFunSuite
import requests.{ RequestFailedException, headers }

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

  test("CatalinaHttp.send should fail when no body is sent") {

    val cat = new CatalinaHttpBase {}

    withServer(cat) { host =>

      val uuid = UUID.randomUUID()

      val res = Try(requests.post(s"$host/send/$uuid"))
        .recover { case e: RequestFailedException => e.response }
        .get

      assert(res.statusCode == 400)
      assert(res.text() == """{"status":400,"message":"BadRequest","data":"Empty body"}""")

    }

  }

  test("CatalinaHttp.send should fail when no proper x-pk is present") {

    val cat = new CatalinaHttpBase {}

    withServer(cat) { host =>

      val uuid = UUID.randomUUID()

      val data = """{"cities":["New York","Bangalore","San Francisco"],"name":"Pankaj Kumar","age":32}""".stripMargin

      val res = Try(requests.post(s"$host/send/$uuid", data = data))
        .recover { case e: RequestFailedException => e.response }
        .get

      assert(res.statusCode == 400)
      assert(res.text() == """{"status":400,"message":"BadRequest","data":"No x-pk"}""")

    }

  }

  test("CatalinaHttp.send should fail when no proper x-pass is present") {

    val cat = new CatalinaHttpBase {}

    withServer(cat) { host =>

      val uuid = UUID.randomUUID()

      val data = """{"cities":["New York","Bangalore","San Francisco"],"name":"Pankaj Kumar","age":32}""".stripMargin

      val res = Try(requests.post(s"$host/send/$uuid", data = data, headers = Map("x-pk" -> "1234567")))
        .recover { case e: RequestFailedException => e.response }
        .get

      assert(res.statusCode == 400)
      assert(res.text() == """{"status":400,"message":"BadRequest","data":"No x-pass"}""")

    }

  }

  test("CatalinaHttp.send should fail when invalid keys") {

    val cat = new CatalinaHttpBase {}

    withServer(cat) { host =>

      val uuid = UUID.randomUUID()

      val data = """{"cities":["New York","Bangalore","San Francisco"],"name":"Pankaj Kumar","age":32}""".stripMargin

      val res = Try(requests.post(
        s"$host/send/$uuid",
        data = data,
        headers = Map("x-pk" -> "1234567", "x-pass" -> "12345678")
      ))
        .recover { case e: RequestFailedException => e.response }
        .get

      assert(res.statusCode == 500)
      assert(res.text() == """{"status":500,"message":"InternalError","data":""}""")

    }

  }

}
