package com.flatmappable.util

import org.apache.http.client.ResponseHandler
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.impl.client.{ CloseableHttpClient, HttpClients }
import org.apache.http.util.EntityUtils
import org.apache.http.{ Header, HttpResponse }

case class ResponseData[T](status: Int, headers: Array[Header], body: T)

trait RequestClient {

  private val httpclient: CloseableHttpClient = HttpClients.createDefault

  sys.addShutdownHook(httpclient.close())

  def call[T](request: HttpRequestBase, responseHandler: ResponseHandler[T]): T = {
    httpclient.execute(request, responseHandler)
  }

  def call(request: HttpRequestBase): ResponseData[Array[Byte]] = {
    call(request, (httpResponse: HttpResponse) => ResponseData(
      httpResponse.getStatusLine.getStatusCode,
      httpResponse.getAllHeaders,
      EntityUtils.toByteArray(httpResponse.getEntity)
    ))
  }

  def callAsString(request: HttpRequestBase): ResponseData[String] = {
    call(request, (httpResponse: HttpResponse) => ResponseData(
      httpResponse.getStatusLine.getStatusCode,
      httpResponse.getAllHeaders,
      EntityUtils.toString(httpResponse.getEntity)
    ))
  }

}
