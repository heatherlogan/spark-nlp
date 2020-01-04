package com.johnsnowlabs.storage

import com.johnsnowlabs.nlp.util.LruMap

trait StorageReader[A] extends HasConnection {

  protected val caseSensitiveIndex: Boolean
  protected def cacheSize: Int

  @transient val lru = new LruMap[String, A](cacheSize)

  def emptyValue: A

  def fromBytes(source: Array[Byte]): A

  private def lookupByIndex(index: String): Option[A] = {
    lazy val resultLower = connection.getDb.get(index.trim.toLowerCase.getBytes())
    lazy val resultUpper = connection.getDb.get(index.trim.toUpperCase.getBytes())
    lazy val resultExact = connection.getDb.get(index.trim.getBytes())

    if (resultExact != null)
      Some(fromBytes(resultExact))
    else if (caseSensitiveIndex && resultLower != null)
      Some(fromBytes(resultLower))
    else if (caseSensitiveIndex && resultExact != null)
      Some(fromBytes(resultExact))
    else if (caseSensitiveIndex && resultUpper != null)
      Some(fromBytes(resultUpper))
    else
      None
  }

  def lookup(index: String): Option[A] = {
    lru.getOrElseUpdate(index, lookupByIndex(index))
  }

  def containsIndex(index: String): Boolean = {
    val wordBytes = index.trim.getBytes()
    connection.getDb.get(wordBytes) != null ||
      (connection.getDb.get(index.trim.toLowerCase.getBytes()) != null) ||
      (connection.getDb.get(index.trim.toUpperCase.getBytes()) != null)
  }

}