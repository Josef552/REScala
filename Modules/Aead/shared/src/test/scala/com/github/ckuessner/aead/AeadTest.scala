package com.github.ckuessner.aead

import com.github.ckuessner.aead.ByteArray
import com.github.ckuessner.aead.Generators.byteArrayGen
import com.sun.net.httpserver.Authenticator.Failure
import org.scalacheck.Prop.*

import scala.concurrent.{ExecutionContext, Future}
import scala.util
import scala.util.{Success, Try}

class AeadTest extends munit.ScalaCheckSuite {

  implicit def executionContext: ExecutionContext = TestExecutionContext.executionContext

  test("AeadHelper.ready should  work") {
    AeadHelper.ready().map(_ => ())(executionContext)
  }

  test("decrypt should successfully decrypt generated byte array and empty associated data") {
    val aead = AeadKey.generateKey.aeadPrimitive
    forAll(byteArrayGen) { (message: ByteArray) =>
      val ciphertext = aead.encrypt(message, Generators.emptyByteArray).get
      val decrypted  = aead.decrypt(ciphertext, Generators.emptyByteArray).get
      assert(decrypted.toArray.sameElements(message.toArray))
    }
  }

  property("decrypt should successfully decrypt generated byte array and associated data") {
    val aead = AeadKey.generateKey.aeadPrimitive
    forAll(byteArrayGen, byteArrayGen) { (message: ByteArray, associatedData: ByteArray) =>
      val ciphertext = aead.encrypt(message, associatedData).get
      val decrypted  = aead.decrypt(ciphertext, associatedData).get
      assert(decrypted.toArray.sameElements(message.toArray))
    }
  }

  test("should work for empty message") {
    val aead = AeadKey.generateKey.aeadPrimitive

    assertEquals("", aead.decrypt(aead.encrypt("", "").get, "").get)
    assertEquals("", aead.decrypt(aead.encrypt("", "Hello").get, "Hello").get)

    aead.decrypt(
      aead.encrypt(Generators.emptyByteArray, Generators.emptyByteArray).get,
      Generators.emptyByteArray
    ) match {
      case Success(arr: ByteArray) => assert(arr.toArray.sameElements(Generators.emptyByteArray))
      case _                       => fail("fail")
    }

    aead.decrypt(
      aead.encrypt(Generators.emptyByteArray, Generators.emptyByteArray).get,
      Generators.emptyByteArray
    ) match {
      case Success(arr: ByteArray) => assert(arr.toArray.sameElements(Generators.emptyByteArray))
      case _                       => fail("fail")
    }
  }

  property("should not work with different key") {
    val aead             = AeadKey.generateKey.aeadPrimitive
    val aeadDifferentKey = AeadKey.generateKey.aeadPrimitive
    forAll(byteArrayGen, byteArrayGen) { (message: ByteArray, associatedData: ByteArray) =>
      val ciphertext = aead.encrypt(message, associatedData).get
      assert(aeadDifferentKey.decrypt(ciphertext, associatedData).isFailure)
    }
  }

  test("should work with encrypted messages from Tink") {
    val key        = "+9HPAi2v5tx8m98fyg50BfQzGQLMYRS9NRy4ESCR9dg="
    val ciphertext = "lYA9rZjZ9U8Pwibtkt7Q4KS8ADEQ4taJHYkLrEuzQeMA0z1el6ZBC1Ui25zlDeA="
    val aead       = AeadKey.fromRawKey(key).aeadPrimitive
    assertEquals(Success("Message"), aead.decrypt(AeadHelper.fromBase64(ciphertext), "Associated Data"))
  }

  test("should work with encrypted messages from Libsodium") {
    val key        = "y1tzzRGeFrTCYfy0GY/FYxsJE9NxSmAlofm2+JYWycQ="
    val ciphertext = "SecoKadHFwiHFBWpCmXxq+UKyoGCsf0cXaGG2fcMgTtHDg6/uzfYP1e1ixZ9sQ0="
    val aead       = AeadKey.fromRawKey(key).aeadPrimitive
    assertEquals(Success("Message"), aead.decrypt(AeadHelper.fromBase64(ciphertext), "Associated Data"))
  }

  property("encrypt should produce different results for two encryptions with same message, associated data and key") {
    val aead = AeadKey.generateKey.aeadPrimitive
    forAll(byteArrayGen, byteArrayGen) { (message: ByteArray, associatedData: ByteArray) =>
      val ciphertext1 = aead.encrypt(message, associatedData).get
      val ciphertext2 = aead.encrypt(message, associatedData).get
      assert(!ciphertext1.toArray.sameElements(ciphertext2))
    }
  }

}
