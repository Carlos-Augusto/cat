package com.flatmappable.util

import org.json4s.jackson.Serialization
import org.json4s.{ Formats, NoTypeHints }

trait WithJsonFormats {
  implicit lazy val formats: Formats = Serialization.formats(NoTypeHints) ++ org.json4s.ext.JavaTypesSerializers.all
}
