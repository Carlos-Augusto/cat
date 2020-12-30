package com.flatmappable

import java.util.UUID

import com.flatmappable.util.Configs
import org.joda.time.{ DateTime, DateTimeZone }
import org.scalatest.funsuite.AnyFunSuite

class DataStoreSpec extends AnyFunSuite {

  test("DataStore should have pending scripts") {
    object db extends DataStore
    assert(db.pending())
  }

  test("DataStore should not have pending scripts") {
    object db extends DataStore
    db.migrate()
    assert(!db.pending())
  }

  test("DataStore should create KeyRow and retrieve it") {
    object db extends DataStore
    db.migrate()

    val id = UUID.randomUUID()
    val keyToCreate = db.KeyRow(id, Configs.ENV, UUID.randomUUID(), "algo", "privKey", "rawPrivKey", "rawPubKey", DateTime.now(DateTimeZone.UTC))
    db.Keys.insert(keyToCreate)
    val keys = db.Keys.byId(id)
    assert(keys.nonEmpty)
    assert(List(keyToCreate) == keys)

  }

}
