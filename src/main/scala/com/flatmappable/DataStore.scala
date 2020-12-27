package com.flatmappable

import com.flatmappable.util.Configs
import io.getquill.{ MappedEncoding, SnakeCase, SqliteJdbcContext }
import io.getquill.context.sql.SqlContext
import org.flywaydb.core.Flyway

import java.util.UUID

trait CustomEncodingsBase {

  implicit def encodeUUID = MappedEncoding[UUID, String](Option(_).map(_.toString).getOrElse(""))
  implicit def decodeUUID = MappedEncoding[String, UUID](x => UUID.fromString(x))

  implicit def encodeSymbol = MappedEncoding[Symbol, String](Option(_).map(_.name).getOrElse(""))
  implicit def decodeSymbol = MappedEncoding[String, Symbol](x => Symbol(x))

}

trait KeyRowDAO extends CustomEncodingsBase {

  val context: SqlContext[_, _]

  import context._

  case class KeyRow(id: UUID, env: Symbol, uuid: UUID, algo: String, privKey: String, rawPrivKey: String, rawPubKey: String)

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

  case class TimestampRow(id: UUID, env: Symbol, uuid: UUID, hash: String, upp: String)

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
  private val flyway: Flyway = Flyway.configure.dataSource(Configs.DB_JDBC_URL, "", "").load

  def migrate() = flyway.migrate()
  def info() = flyway.info()
  def pending() = info.pending().nonEmpty

}

trait DataStore extends KeyRowDAO with TimestampRowDAO with DBMigration {
  override val context = new SqliteJdbcContext(SnakeCase, "db")

  import context._

  object KeyRowQueriesImp extends KeyRowQueries {
    override def insert(keyRow: KeyRow): Long = context.run(KeyRow.insertQ(lift(keyRow)))
  }

  object TimestampRowQueriesImp extends TimestampRowQueries {
    override def insert(timestampRow: TimestampRow): Long = context.run(TimestampRow.insertQ(lift(timestampRow)))
  }

}

