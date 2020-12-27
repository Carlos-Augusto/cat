package com.flatmappable

import com.flatmappable.util.Configs
import io.getquill.{ MappedEncoding, SnakeCase, SqliteDialect, SqliteJdbcContext }
import io.getquill.context.sql.SqlContext
import org.flywaydb.core.Flyway

import java.util.UUID

trait CustomEncodingsBase {

  implicit def encodeUUID = MappedEncoding[UUID, String](Option(_).map(_.toString).getOrElse(""))
  implicit def decodeUUID = MappedEncoding[String, UUID](x => UUID.fromString(x))

}

trait KeyRowDAO extends CustomEncodingsBase {

  val context: SqlContext[_, _]

  import context._

  case class KeyRow(id: UUID, uuid: UUID, algo: String, privKey: String, rawPrivKey: String, rawPubKey: String)

  object KeyRow {

    val key = quote {
      query[KeyRow]
    }

    val insertQ = quote {
      (k: KeyRow) => query[KeyRow].insert(k)
    }

  }

  trait KeyRowQueries {
    def insert(keyRow: KeyRow): Long
  }

}

trait DBMigration {
  private val flyway: Flyway = Flyway.configure.dataSource(Configs.DB_JDBC_URL, "", "").load

  def migrate() = flyway.migrate()
  def info() = flyway.info()
  def pending() = info.pending().nonEmpty

}

trait DataStore extends KeyRowDAO with DBMigration {
  override val context = new SqliteJdbcContext(SnakeCase, "db")

  import context._

  object KeyRowQueriesImp extends KeyRowQueries {
    override def insert(keyRow: KeyRow): Long = context.run(KeyRow.insertQ(lift(keyRow)))
  }

}

