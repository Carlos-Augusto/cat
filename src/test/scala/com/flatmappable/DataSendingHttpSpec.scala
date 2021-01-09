package com.flatmappable

import java.nio.charset.{ Charset, StandardCharsets }
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

  test("CatalinaHttp.send should fail when invalid uuid is detected") {

    val cat = new CatalinaHttpBase {}

    withServer(cat) { host =>

      val res = Try(requests.post(s"$host/send/123456"))
        .recover { case e: RequestFailedException => e.response }
        .get

      assert(res.statusCode == 400)
      assert(res.text() == """{"status":400,"message":"BadRequest","data":"Invalid uuid"}""")

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

      override protected def sendBody(contentType: Option[String], charset: Option[Charset], headers: Map[String, collection.Seq[String]], body: Array[Byte]): (String, Array[Byte]) = {
        val (a, b) = super.sendBody(contentType, charset, headers, body)
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

      override protected def sendBody(contentType: Option[String], charset: Option[Charset], headers: Map[String, collection.Seq[String]], body: Array[Byte]): (String, Array[Byte]) = {
        val (a, b) = super.sendBody(contentType, charset, headers, body)
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

      override protected def sendBody(contentType: Option[String], charset: Option[Charset], headers: Map[String, collection.Seq[String]], body: Array[Byte]): (String, Array[Byte]) = {
        val (a, b) = super.sendBody(contentType, charset, headers, body)
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

      override protected def sendBody(contentType: Option[String], charset: Option[Charset], headers: Map[String, collection.Seq[String]], body: Array[Byte]): (String, Array[Byte]) = {
        val (a, b) = super.sendBody(contentType, charset, headers, body)
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

      override protected def sendBody(contentType: Option[String], charset: Option[Charset], headers: Map[String, collection.Seq[String]], body: Array[Byte]): (String, Array[Byte]) = {
        val (a, b) = super.sendBody(contentType, charset, headers, body)
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

      override protected def sendBody(contentType: Option[String], charset: Option[Charset], headers: Map[String, collection.Seq[String]], body: Array[Byte]): (String, Array[Byte]) = {
        val (a, b) = super.sendBody(contentType, charset, headers, body)
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

  test("CatalinaHttp.send should detect json with maybe special character") {

    var processed: String = null

    val cat = new CatalinaHttpBase {
      override def sendData(uuid: UUID, password: String, hash: Array[Byte], upp: Array[Byte], extraHeaders: Map[String, Seq[String]]): ResponseData[Array[Byte]] = {
        ResponseData(200, Array.empty, Array.empty[Byte])
      }

      override protected def sendBody(contentType: Option[String], charset: Option[Charset], headers: Map[String, collection.Seq[String]], body: Array[Byte]): (String, Array[Byte]) = {
        val (a, b) = super.sendBody(contentType, charset, headers, body)
        processed = a
        (a, b)
      }
    }

    withServer(cat) { host =>

      val data = """{"data":"verfügbar, nämlich"}""".stripMargin

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

  test("CatalinaHttp.send should detect json with more special characters") {

    var processed: String = null

    val cat = new CatalinaHttpBase {
      override def sendData(uuid: UUID, password: String, hash: Array[Byte], upp: Array[Byte], extraHeaders: Map[String, Seq[String]]): ResponseData[Array[Byte]] = {
        ResponseData(200, Array.empty, Array.empty[Byte])
      }

      override protected def sendBody(contentType: Option[String], charset: Option[Charset], headers: Map[String, collection.Seq[String]], body: Array[Byte]): (String, Array[Byte]) = {
        val (a, b) = super.sendBody(contentType, charset, headers, body)
        processed = a
        (a, b)
      }
    }

    withServer(cat) { host =>

      val data = """{"data":"let's do it, ich möchte, nämlich"}""".stripMargin

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

  test("CatalinaHttp.send should detect text with maybe special character") {

    var processed: String = null

    val cat = new CatalinaHttpBase {
      override def sendData(uuid: UUID, password: String, hash: Array[Byte], upp: Array[Byte], extraHeaders: Map[String, Seq[String]]): ResponseData[Array[Byte]] = {
        ResponseData(200, Array.empty, Array.empty[Byte])
      }

      override protected def sendBody(contentType: Option[String], charset: Option[Charset], headers: Map[String, collection.Seq[String]], body: Array[Byte]): (String, Array[Byte]) = {
        val (a, b) = super.sendBody(contentType, charset, headers, body)
        processed = a
        (a, b)
      }
    }

    withServer(cat) { host =>

      val data = """{"data":"verfügbar, nämlich"}""".stripMargin

      val res = Try(requests.post(
        s"$host/send/23949125-e476-4e06-b72c-5dde2cc247b0",
        data = data,
        headers = Map(
          "x-pk" -> "hcOakLL7KO6XmsdZYQdb9uZeO5/IwxqmgAudIzXQpgE=",
          "x-pass" -> "12345678",
          "content-type" -> "text/plain"
        )
      ))
        .recover { case e: RequestFailedException => e.response }
        .get

      assert(res.statusCode == 200)
      assert(processed == data)

    }

  }

  test("CatalinaHttp.send should detect text") {

    var processed: String = null

    val cat = new CatalinaHttpBase {
      override def sendData(uuid: UUID, password: String, hash: Array[Byte], upp: Array[Byte], extraHeaders: Map[String, Seq[String]]): ResponseData[Array[Byte]] = {
        ResponseData(200, Array.empty, Array.empty[Byte])
      }

      override protected def sendBody(contentType: Option[String], charset: Option[Charset], headers: Map[String, collection.Seq[String]], body: Array[Byte]): (String, Array[Byte]) = {
        val (a, b) = super.sendBody(contentType, charset, headers, body)
        processed = a
        (a, b)
      }
    }

    withServer(cat) { host =>

      val data =
        """
          |Ein Automobil, kurz Auto (auch Kraftwagen, in der Schweiz amtlich Motorwagen), ist ein mehrspuriges Kraftfahrzeug (also ein von einem Motor angetriebenes Straßenfahrzeug), das zur Beförderung von Personen (Personenkraftwagen „Pkw“ und Bus) oder Frachtgütern (Lastkraftwagen „Lkw“) dient. Umgangssprachlich – und auch in diesem Artikel – werden mit dem Begriff Auto meist Fahrzeuge bezeichnet, deren Bauart überwiegend zur Personenbeförderung bestimmt ist und die mit einem Automobil-Führerschein auf öffentlichem Verkehrsgrund geführt werden dürfen.
          |
          |Der weltweite Fahrzeugbestand steigt kontinuierlich an und lag im Jahr 2010 bei über 1,015 Milliarden Automobilen. 2011 wurden weltweit über 80 Millionen Automobile gebaut. In Deutschland waren im Jahr 2012 etwa 51,7 Millionen Kraftfahrzeuge zugelassen, davon sind knapp 43 Millionen Personenkraftwagen.
          |""".stripMargin

      val res = Try(requests.post(
        s"$host/send/23949125-e476-4e06-b72c-5dde2cc247b0",
        data = data,
        headers = Map(
          "x-pk" -> "hcOakLL7KO6XmsdZYQdb9uZeO5/IwxqmgAudIzXQpgE=",
          "x-pass" -> "12345678",
          "content-type" -> "text/plain"
        )
      ))
        .recover { case e: RequestFailedException => e.response }
        .get

      assert(res.statusCode == 200)
      assert(processed == data)

    }

  }

  test("CatalinaHttp.send should detect text with charset") {

    var processed: String = null

    val cat = new CatalinaHttpBase {
      override def sendData(uuid: UUID, password: String, hash: Array[Byte], upp: Array[Byte], extraHeaders: Map[String, Seq[String]]): ResponseData[Array[Byte]] = {
        ResponseData(200, Array.empty, Array.empty[Byte])
      }

      override protected def sendBody(contentType: Option[String], charset: Option[Charset], headers: Map[String, collection.Seq[String]], body: Array[Byte]): (String, Array[Byte]) = {
        val (a, b) = super.sendBody(contentType, charset, headers, body)
        processed = a
        (a, b)
      }
    }

    withServer(cat) { host =>

      val data = "hola"

      val res = Try(requests.post(
        s"$host/send/23949125-e476-4e06-b72c-5dde2cc247b0",
        data = data.getBytes(StandardCharsets.US_ASCII),
        headers = Map(
          "x-pk" -> "hcOakLL7KO6XmsdZYQdb9uZeO5/IwxqmgAudIzXQpgE=",
          "x-pass" -> "12345678",
          "content-type" -> "text/plain; charset=US-ASCII"
        )
      ))
        .recover { case e: RequestFailedException => e.response }
        .get

      assert(res.statusCode == 200)
      assert(processed == data)

    }

  }

}
