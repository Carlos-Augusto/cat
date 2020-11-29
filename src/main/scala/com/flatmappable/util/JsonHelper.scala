package com.flatmappable
package util

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

import org.json4s.JValue
import org.json4s.jackson.JsonMethods

import scala.util.Try

object JsonHelper {

  def use(bytes: Array[Byte]): Either[Exception, JValue] = {
    closableTry(new ByteArrayInputStream(bytes))(_.close()) { is =>
      JsonMethods.parse(is)
    }
  }

  def compact(bytes: Array[Byte]): Either[Throwable, (String, Array[Byte])] = {
    for {
      jv <- use(bytes)
      jvs <- Try(JsonMethods.compact(jv)).toEither
      b <- Try(jvs.getBytes(StandardCharsets.UTF_8)).toEither
    } yield {
      (jvs, b)
    }
  }

  def pretty(bytes: Array[Byte]): Either[Throwable, (String, Array[Byte])] = {
    for {
      jv <- use(bytes)
      jvs <- Try(JsonMethods.pretty(jv)).toEither
      b <- Try(jvs.getBytes(StandardCharsets.UTF_8)).toEither
    } yield {
      (jvs, b)
    }
  }

}
