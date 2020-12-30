package com.flatmappable

import java.util.UUID

import com.flatmappable.util.Configs
import org.joda.time.{ DateTime, DateTimeZone }
import org.scalatest.funsuite.AnyFunSuite

class DataStoreSpec extends AnyFunSuite {

  test("DataStore should have valid state in evolution scripts") {
    object db extends DataStore
    assert(db.pending())
    db.migrate()
    assert(!db.pending())
  }

  test("DataStore.Keys should create KeyRow and retrieve it") {
    object db extends DataStore
    db.migrate()

    val id = UUID.randomUUID()
    val keyToCreate = db.KeyRow(id, Configs.ENV, UUID.randomUUID(), "algo", "privKey", "rawPrivKey", "rawPubKey", DateTime.now(DateTimeZone.UTC))
    db.Keys.insert(keyToCreate)
    val keys = db.Keys.byId(id)
    assert(keys.nonEmpty)
    assert(List(keyToCreate) == keys)
    assertThrows[org.sqlite.SQLiteException](db.Keys.insert(keyToCreate)) // no duplicates

  }

  test("DataStore.Timestamps should create timestamp and retrieve it") {
    object db extends DataStore
    db.migrate()

    val id = UUID.randomUUID()
    val timestampsToCreate = db.TimestampRow(id, Configs.ENV, UUID.randomUUID(), "hash", "upp", DateTime.now(DateTimeZone.UTC))
    db.Timestamps.insert(timestampsToCreate)
    val timestamps = db.Timestamps.byId(id)
    assert(timestamps.nonEmpty)
    assert(List(timestampsToCreate) == timestamps)
    assertThrows[org.sqlite.SQLiteException](db.Timestamps.insert(timestampsToCreate)) // no duplicates

  }

}
