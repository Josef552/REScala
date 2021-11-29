package de.ckuessner
package encrdt.benchmarks

import encrdt.benchmarks.Codecs.awlwwmapJsonCodec
import encrdt.causality.VectorClock
import encrdt.crdts.AddWinsLastWriterWinsMap
import encrdt.encrypted.statebased.DecryptedState.vectorClockJsonCodec

import com.github.plokhotnyuk.jsoniter_scala.core.writeToArray
import com.google.crypto.tink.Aead
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole

import java.util.concurrent.TimeUnit

@OutputTimeUnit(TimeUnit.MILLISECONDS)
@BenchmarkMode(Array(Mode.AverageTime))
@State(Scope.Thread)
@Warmup(iterations = 4, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Fork(5)
@Threads(1)
class AWLWWMapBenchmark {
  val replicaId = "TestReplica"

  @Benchmark
  def serializeOnly(blackhole: Blackhole, serializeOnlyBenchmarkState: SerializeOnlyBenchmarkState): Unit = {
    val serialPlaintextState = writeToArray(serializeOnlyBenchmarkState.crdtState)
    blackhole.consume(serialPlaintextState)
  }

  @Benchmark
  @Warmup(iterations = 7)
  def encryptOnly(blackhole: Blackhole, serializeOnlyBenchmarkState: SerializeOnlyBenchmarkState, aeadState: AeadState): Unit = {
    val serialEncryptedState = aeadState.aead.encrypt(serializeOnlyBenchmarkState.serialPlaintextState, serializeOnlyBenchmarkState.serialPlaintextVectorClock)
    blackhole.consume(serialEncryptedState, serializeOnlyBenchmarkState.serialPlaintextState)
  }

  @Benchmark
  @Warmup(iterations = 10)
  def serializeAndEncrypt(blackhole: Blackhole, serializeOnlyBenchmarkState: SerializeOnlyBenchmarkState, aeadState: AeadState): Unit = {
    val serialPlaintextState = writeToArray(serializeOnlyBenchmarkState.crdtState)
    val serialPlaintextVectorClock = writeToArray(serializeOnlyBenchmarkState.crdtStateVersionVector)
    val serialEncryptedState = aeadState.aead.encrypt(serialPlaintextState, serialPlaintextVectorClock)
    blackhole.consume(serialEncryptedState, serialPlaintextState)
  }

  @Benchmark
  def putOnceNoSerialization(benchState: SerializeOnlyBenchmarkState): Unit = {
    benchState.crdt.put("This is a String", "And so this is too")
  }

  @Benchmark
  @Fork(2)
  @Warmup(iterations = 3, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
  def putAndSerializeManyTimes(blackhole: Blackhole, putBenchmarkState: PutManyBenchmarkState): Unit = {
    val crdt = new AddWinsLastWriterWinsMap[String, String](replicaId)
    for (entry <- putBenchmarkState.dummyKeyValuePairs) {
      // Update crdt
      crdt.put(entry._1, entry._2)
      // Serialize to JSON (as byte array)
      val serializedState = writeToArray(crdt.state)

      blackhole.consume(serializedState)
    }
  }

  @Benchmark
  @Fork(2)
  @Warmup(iterations = 3, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
  def putAndSerializeAndEncryptManyTimes(blackhole: Blackhole, putBenchmarkState: PutManyBenchmarkState, aeadState: AeadState): Unit = {
    var versionVector: VectorClock = VectorClock()
    val crdt = new AddWinsLastWriterWinsMap[String, String](replicaId)
    val aead = aeadState.aead

    for (entry <- putBenchmarkState.dummyKeyValuePairs) {
      // Update crdt
      crdt.put(entry._1, entry._2)
      // Track causality information used for encrypted crdt
      versionVector = versionVector.advance(replicaId)
      // Serialize/Encrypt/Authenticate state with attached causality
      val serialState = writeToArray(crdt.state)
      val serialVectorClock = writeToArray(versionVector)
      val encryptedState = aead.encrypt(serialState, serialVectorClock)

      blackhole.consume(encryptedState, serialState)
    }
  }
}

@State(Scope.Thread)
class AeadState {
  @Param(Array("AES128_GCM", "AES256_GCM", "AES256_GCM_SIV", "XCHACHA20_POLY1305"))
  var keyTemplateString: String = _

  var aead: Aead = _

  @Setup(Level.Trial)
  def setupAead(): Unit = {
    aead = Helper.setupAead(keyTemplateString)
  }
}

@State(Scope.Thread)
class SerializeOnlyBenchmarkState {
  var crdt: AddWinsLastWriterWinsMap[String, String] = _
  var crdtState: AddWinsLastWriterWinsMap.LatticeType[String, String] = _
  var crdtStateVersionVector: VectorClock = _

  var serialPlaintextState: Array[Byte] = _
  var serialPlaintextVectorClock: Array[Byte] = _

  @Param(Array("10", "100", "1000", "10000"))
  var crdtSizeInElements: Int = _

  @Setup(Level.Trial)
  def setupCrdtState(): Unit = {
    val dummyKeyValuePairs = Helper.dummyKeyValuePairs(crdtSizeInElements)

    var versionVector: VectorClock = VectorClock()
    val replicaId = "TestReplica"
    val crdt = new AddWinsLastWriterWinsMap[String, String](replicaId)

    for (entry <- dummyKeyValuePairs) {
      // Update crdt
      crdt.put(entry._1, entry._2)
      // Track causality information used for encrypted crdt
      versionVector = versionVector.advance(replicaId)
    }

    this.crdt = crdt
    this.crdtState = crdt.state
    this.crdtStateVersionVector = versionVector

    this.serialPlaintextState = writeToArray(crdtState)
    this.serialPlaintextVectorClock = writeToArray(crdtStateVersionVector)
  }
}

@State(Scope.Thread)
class PutManyBenchmarkState {
  var dummyKeyValuePairs: Array[(String, String)] = _

  @Param(Array("10", "100", "1000"))
  var crdtSizeInElements: Int = _

  @Setup(Level.Trial)
  def setupTestData(): Unit = {
    dummyKeyValuePairs = Helper.dummyKeyValuePairs(crdtSizeInElements)
  }
}

