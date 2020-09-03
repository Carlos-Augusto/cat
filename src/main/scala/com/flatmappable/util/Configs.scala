package com.flatmappable.util

object EnvConfigs extends ConfigBase {
  val ENV: String = conf.getString("environment")
}
