package com.flatmappable

import java.util.UUID

import com.flatmappable.util.ResponseData
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

  test("CatalinaHttp.send should fail when not authorized") {

    val cat = new CatalinaHttpBase {
      override def sendData(uuid: UUID, password: String, hash: Array[Byte], upp: Array[Byte], extraHeaders: Map[String, Seq[String]]): ResponseData[Array[Byte]] = {
        ResponseData(401, Array.empty, Array.empty)
      }
    }

    withServer(cat) { host =>

      val data = """{"cities":["New York","Bangalore","San Francisco"],"name":"Pankaj Kumar","age":32}""".stripMargin

      val res = Try(requests.post(
        s"$host/send/23949125-e476-4e06-b72c-5dde2cc247b0",
        data = data,
        headers = Map("x-pk" -> "hcOakLL7KO6XmsdZYQdb9uZeO5/IwxqmgAudIzXQpgE=", "x-pass" -> "12345678")
      ))
        .recover { case e: RequestFailedException => e.response }
        .get

      assert(res.statusCode == 401)

    }

  }

  test("CatalinaHttp.send should forward x-proxy headers") {

    val cat = new CatalinaHttpBase {
      override def sendData(uuid: UUID, password: String, hash: Array[Byte], upp: Array[Byte], extraHeaders: Map[String, Seq[String]]): ResponseData[Array[Byte]] = {
        extraHeaders.find { case (k, v) => k == "x-token" && v == Seq("this is an OK token") }.map { _ =>
          ResponseData(200, Array.empty, Array.empty[Byte])
        }.getOrElse(ResponseData(500, Array.empty, Array.empty[Byte]))
      }
    }

    withServer(cat) { host =>

      val data = """{"cities":["New York","Bangalore","San Francisco"],"name":"Pankaj Kumar","age":32}""".stripMargin

      val res = Try(requests.post(
        s"$host/send/23949125-e476-4e06-b72c-5dde2cc247b0",
        data = data,
        headers = Map(
          "x-pk" -> "hcOakLL7KO6XmsdZYQdb9uZeO5/IwxqmgAudIzXQpgE=",
          "x-pass" -> "12345678",
          "x-proxy-x-token" -> "this is an OK token",
          "x-proxy-x-" -> "this is a NOT OK token",
          "x-proxy--" -> "this is a NOT OK token",
          "x-proxy---" -> "this is a NOT OK token",
          "x-proxy-x-aaa" -> "",
          "x-proxy-x-bbb" -> " "
        )
      ))
        .recover { case e: RequestFailedException => e.response }
        .get

      assert(res.statusCode == 200)

    }

  }

  test("CatalinaHttp.send should detect json content type header but no formatting option") {

    var processed: String = null

    val cat = new CatalinaHttpBase {
      override def sendData(uuid: UUID, password: String, hash: Array[Byte], upp: Array[Byte], extraHeaders: Map[String, Seq[String]]): ResponseData[Array[Byte]] = {
        ResponseData(200, Array.empty, Array.empty[Byte])
      }

      override protected def sendBody(contentType: Option[String], headers: Map[String, collection.Seq[String]], body: Array[Byte]): (String, Array[Byte]) = {
        val (a, b) = super.sendBody(contentType, headers, body)
        processed = a
        (a, b)
      }
    }

    withServer(cat) { host =>

      val data =
        """
          |{
          |   "cities":[
          |      "New York",
          |      "Bangalore",
          |      "San Francisco"
          |   ],
          |   "name":"Pankaj Kumar",
          |   "age":32
          |}""".stripMargin

      val res = Try(requests.post(
        s"$host/send/23949125-e476-4e06-b72c-5dde2cc247b0",
        data = data,
        headers = Map(
          "x-pk" -> "hcOakLL7KO6XmsdZYQdb9uZeO5/IwxqmgAudIzXQpgE=",
          "x-pass" -> "12345678",
          "content-type" -> "application/json"
        )
      ))
        .recover { case e: RequestFailedException => e.response }
        .get

      val expected = """{"cities":["New York","Bangalore","San Francisco"],"name":"Pankaj Kumar","age":32}""".stripMargin

      assert(res.statusCode == 200)
      assert(processed == expected)

    }

  }

  test("CatalinaHttp.send should detect json content type header but no formatting option 2") {

    var processed: String = null

    val cat = new CatalinaHttpBase {
      override def sendData(uuid: UUID, password: String, hash: Array[Byte], upp: Array[Byte], extraHeaders: Map[String, Seq[String]]): ResponseData[Array[Byte]] = {
        ResponseData(200, Array.empty, Array.empty[Byte])
      }

      override protected def sendBody(contentType: Option[String], headers: Map[String, collection.Seq[String]], body: Array[Byte]): (String, Array[Byte]) = {
        val (a, b) = super.sendBody(contentType, headers, body)
        processed = a
        (a, b)
      }
    }

    withServer(cat) { host =>

      val data = """{"cities":["New York","Bangalore","San Francisco"],"name":"Pankaj Kumar","age":32}""".stripMargin

      val res = Try(requests.post(
        s"$host/send/23949125-e476-4e06-b72c-5dde2cc247b0",
        data = data,
        headers = Map(
          "x-pk" -> "hcOakLL7KO6XmsdZYQdb9uZeO5/IwxqmgAudIzXQpgE=",
          "x-pass" -> "12345678",
          "content-type" -> "application/json"
        )
      ))
        .recover { case e: RequestFailedException => e.response }
        .get

      assert(res.statusCode == 200)
      assert(processed == data)

    }

  }

  test("CatalinaHttp.send should detect json content type header but no formatting option 3") {

    var processed: String = null

    val cat = new CatalinaHttpBase {
      override def sendData(uuid: UUID, password: String, hash: Array[Byte], upp: Array[Byte], extraHeaders: Map[String, Seq[String]]): ResponseData[Array[Byte]] = {
        ResponseData(200, Array.empty, Array.empty[Byte])
      }

      override protected def sendBody(contentType: Option[String], headers: Map[String, collection.Seq[String]], body: Array[Byte]): (String, Array[Byte]) = {
        val (a, b) = super.sendBody(contentType, headers, body)
        processed = a
        (a, b)
      }
    }

    withServer(cat) { host =>

      val data = """{"cities": ["New York","Bangalore","San Francisco"], "name":"Pankaj Kumar","age":32}""".stripMargin

      val res = Try(requests.post(
        s"$host/send/23949125-e476-4e06-b72c-5dde2cc247b0",
        data = data,
        headers = Map(
          "x-pk" -> "hcOakLL7KO6XmsdZYQdb9uZeO5/IwxqmgAudIzXQpgE=",
          "x-pass" -> "12345678",
          "content-type" -> "application/json"
        )
      ))
        .recover { case e: RequestFailedException => e.response }
        .get

      val expected = """{"cities":["New York","Bangalore","San Francisco"],"name":"Pankaj Kumar","age":32}""".stripMargin
      assert(res.statusCode == 200)
      assert(processed == expected)

    }

  }

  test("CatalinaHttp.send should detect json content type header and invalid json") {

    val cat = new CatalinaHttpBase {
      override def sendData(uuid: UUID, password: String, hash: Array[Byte], upp: Array[Byte], extraHeaders: Map[String, Seq[String]]): ResponseData[Array[Byte]] = {
        ResponseData(200, Array.empty, Array.empty[Byte])
      }
    }

    withServer(cat) { host =>

      val data = """{"cities": ["New York""Bangalore","San Francisco"], "name":"Pankaj Kumar","age":32}""".stripMargin

      val res = Try(requests.post(
        s"$host/send/23949125-e476-4e06-b72c-5dde2cc247b0",
        data = data,
        headers = Map(
          "x-pk" -> "hcOakLL7KO6XmsdZYQdb9uZeO5/IwxqmgAudIzXQpgE=",
          "x-pass" -> "12345678",
          "content-type" -> "application/json"
        )
      ))
        .recover { case e: RequestFailedException => e.response }
        .get

      assert(res.statusCode == 400)
      assert(res.text() == """{"status":400,"message":"BadRequest","data":"Body is malformed"}""")

    }

  }

  test("CatalinaHttp.send should detect json content type header and formatting option none") {

    var processed: String = null

    val cat = new CatalinaHttpBase {
      override def sendData(uuid: UUID, password: String, hash: Array[Byte], upp: Array[Byte], extraHeaders: Map[String, Seq[String]]): ResponseData[Array[Byte]] = {
        ResponseData(200, Array.empty, Array.empty[Byte])
      }

      override protected def sendBody(contentType: Option[String], headers: Map[String, collection.Seq[String]], body: Array[Byte]): (String, Array[Byte]) = {
        val (a, b) = super.sendBody(contentType, headers, body)
        processed = a
        (a, b)
      }
    }

    withServer(cat) { host =>

      val data = """{"cities": ["New York", "Bangalore","San Francisco"], "name":"Pankaj Kumar","age":32}""".stripMargin

      val res = Try(requests.post(
        s"$host/send/23949125-e476-4e06-b72c-5dde2cc247b0",
        data = data,
        headers = Map(
          "x-pk" -> "hcOakLL7KO6XmsdZYQdb9uZeO5/IwxqmgAudIzXQpgE=",
          "x-pass" -> "12345678",
          "content-type" -> "application/json",
          "x-json-format" -> "none"
        )
      ))
        .recover { case e: RequestFailedException => e.response }
        .get

      assert(res.statusCode == 200)
      assert(processed == data)

    }

  }

  test("CatalinaHttp.send should detect json content type header and formatting option compact") {

    var processed: String = null

    val cat = new CatalinaHttpBase {
      override def sendData(uuid: UUID, password: String, hash: Array[Byte], upp: Array[Byte], extraHeaders: Map[String, Seq[String]]): ResponseData[Array[Byte]] = {
        ResponseData(200, Array.empty, Array.empty[Byte])
      }

      override protected def sendBody(contentType: Option[String], headers: Map[String, collection.Seq[String]], body: Array[Byte]): (String, Array[Byte]) = {
        val (a, b) = super.sendBody(contentType, headers, body)
        processed = a
        (a, b)
      }
    }

    withServer(cat) { host =>

      val data = """{"cities": ["New York", "Bangalore","San Francisco"], "name":"Pankaj Kumar","age":32}""".stripMargin

      val res = Try(requests.post(
        s"$host/send/23949125-e476-4e06-b72c-5dde2cc247b0",
        data = data,
        headers = Map(
          "x-pk" -> "hcOakLL7KO6XmsdZYQdb9uZeO5/IwxqmgAudIzXQpgE=",
          "x-pass" -> "12345678",
          "content-type" -> "application/json",
          "x-json-format" -> "compact"
        )
      ))
        .recover { case e: RequestFailedException => e.response }
        .get

      val expected = """{"cities":["New York","Bangalore","San Francisco"],"name":"Pankaj Kumar","age":32}""".stripMargin
      assert(res.statusCode == 200)
      assert(processed == expected)

    }

  }

  test("CatalinaHttp.send should detect json content type header and formatting option pretty") {

    var processed: String = null

    val cat = new CatalinaHttpBase {
      override def sendData(uuid: UUID, password: String, hash: Array[Byte], upp: Array[Byte], extraHeaders: Map[String, Seq[String]]): ResponseData[Array[Byte]] = {
        ResponseData(200, Array.empty, Array.empty[Byte])
      }

      override protected def sendBody(contentType: Option[String], headers: Map[String, collection.Seq[String]], body: Array[Byte]): (String, Array[Byte]) = {
        val (a, b) = super.sendBody(contentType, headers, body)
        processed = a
        (a, b)
      }
    }

    withServer(cat) { host =>

      val data = """{"cities": ["New York", "Bangalore","San Francisco"], "name":"Pankaj Kumar","age":32}""".stripMargin

      val res = Try(requests.post(
        s"$host/send/23949125-e476-4e06-b72c-5dde2cc247b0",
        data = data,
        headers = Map(
          "x-pk" -> "hcOakLL7KO6XmsdZYQdb9uZeO5/IwxqmgAudIzXQpgE=",
          "x-pass" -> "12345678",
          "content-type" -> "application/json",
          "x-json-format" -> "pretty"
        )
      ))
        .recover { case e: RequestFailedException => e.response }
        .get

      val expected =
        """
          |{
          |  "cities" : [ "New York", "Bangalore", "San Francisco" ],
          |  "name" : "Pankaj Kumar",
          |  "age" : 32
          |}
          |""".stripMargin.trim

      assert(res.statusCode == 200)
      assert(processed == expected)

    }

  }

}
