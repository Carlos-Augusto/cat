package com.flatmappable.util

import com.typesafe.config.{ Config, ConfigFactory }

object Configs {
  val conf: Config = ConfigFactory.load()
  val ENV: String = conf.getString("environment")
}
