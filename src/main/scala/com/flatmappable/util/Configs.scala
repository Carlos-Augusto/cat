package com.flatmappable.util

import com.typesafe.config.{ Config, ConfigFactory }

object Configs {
  final val conf: Config = ConfigFactory.load()
  final val ENV: Symbol = Symbol(conf.getString("environment"))
  final val DATA_FOLDER: String = conf.getString("dataFolder")
  final val CAT_HTTP_PORT: Int = conf.getInt("catHttpPort")
  final val CAT_HTTP_HOST: String = conf.getString("catHttpHost")
  final val DATA_SENDING_URL: String = conf.getString("dataSendingUrl")
  final val KEY_REGISTRATION_URL: String = conf.getString("keyRegistrationUrl")
  final val SIMPLE_VERIFICATION_URL: String = conf.getString("simpleVerificationUrl")
  final val INITIAL_VERIFICATION_URL: String = conf.getString("initialVerificationUrl")
  final val UPPER_VERIFICATION_URL: String = conf.getString("upperVerificationUrl")
  final val FULL_VERIFICATION_URL: String = conf.getString("fullVerificationUrl")
  final val DB_CONFIG: Config = conf.getConfig("db")
}
