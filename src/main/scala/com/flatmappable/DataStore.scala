package com.flatmappable

import java.nio.file.Paths
import java.util.{ Date, UUID }

import com.flatmappable.util.Configs
import com.typesafe.config.{ Config, ConfigValueFactory }
import io.getquill.context.sql.SqlContext
import io.getquill.{ MappedEncoding, SnakeCase, SqliteJdbcContext }
import org.flywaydb.core.Flyway

trait CustomEncodingsBase {

  implicit def encodeUUID = MappedEncoding[UUID, String](Option(_).map(_.toString).getOrElse(""))
  implicit def decodeUUID = MappedEncoding[String, UUID](x => UUID.fromString(x))

  implicit def encodeSymbol = MappedEncoding[Symbol, String](Option(_).map(_.name).getOrElse(""))
  implicit def decodeSymbol = MappedEncoding[String, Symbol](x => Symbol(x))

}

trait KeyRowDAO extends CustomEncodingsBase {

  val context: SqlContext[_, _]

  import context._

  case class KeyRow(id: UUID, env: Symbol, uuid: UUID, algo: String, privKey: String, rawPrivKey: String, rawPubKey: String, createdAt: Date)

  object KeyRow {

    val insertQ = quote {
      (k: KeyRow) => query[KeyRow].insert(k)
    }

  }

  trait KeyRowQueries {
    def insert(keyRow: KeyRow): Long
  }

}

trait TimestampRowDAO extends CustomEncodingsBase {

  val context: SqlContext[_, _]

  import context._

  case class TimestampRow(id: UUID, env: Symbol, uuid: UUID, hash: String, upp: String, createdAt: Date)

  object TimestampRow {

    val insertQ = quote {
      (t: TimestampRow) => query[TimestampRow].insert(t)
    }

  }

  trait TimestampRowQueries {
    def insert(timestampRow: TimestampRow): Long
  }

}

trait DBMigration {

  def flyway: Flyway

  def migrate() = flyway.migrate()
  def info() = flyway.info()
  def pending() = info.pending().nonEmpty

}

trait DataStore extends KeyRowDAO with TimestampRowDAO with DBMigration {

  lazy val asTest: Boolean = Configs.DB_CONFIG.getBoolean("asTest")
  lazy val jdbcUrl: String = {
    val home = if (asTest) Paths.get(System.getProperty("java.io.tmpdir")) else PATH_HOME
    val name = if (asTest) s"cat.$now.db" else "cat.db"
    s"jdbc:sqlite:${home.resolve(name).normalize().toFile.toString}"
  }
  lazy val dbConfig: Config = Configs.DB_CONFIG
    .withValue("jdbcUrl", ConfigValueFactory.fromAnyRef(jdbcUrl))
    .withoutPath("asTest") // We remove this path as the db pool complains otherwise

  override lazy val flyway: Flyway = Flyway.configure.dataSource(jdbcUrl, "", "").load
  override lazy val context = new SqliteJdbcContext(SnakeCase, dbConfig)

  import context._

  object Keys extends KeyRowQueries {
    override def insert(keyRow: KeyRow): Long = context.run(KeyRow.insertQ(lift(keyRow)))
  }

  object Timestamps extends TimestampRowQueries {
    override def insert(timestampRow: TimestampRow): Long = context.run(TimestampRow.insertQ(lift(timestampRow)))
  }

  sys.addShutdownHook(context.close())

}

