package de.ckuessner
package encrdt.benchmarks

import encrdt.benchmarks.Codecs.deltaAwlwwmapJsonCodec
import encrdt.causality.VectorClock
import encrdt.crdts.DeltaAddWinsLastWriterWinsMap
import encrdt.crdts.DeltaAddWinsLastWriterWinsMap.{StateType, timestampedValueLattice}
import encrdt.encrypted.statebased.{DecryptedState, EncryptedState, UntrustedReplica}

import com.github.plokhotnyuk.jsoniter_scala.core.writeToArray

import java.io.PrintWriter
import java.nio.file.{Files, Paths}

object StateBasedUntrustedReplicaSizeBenchmark extends App {
  val MAX_TESTED_ELEMENTS = 10_000
  val MAX_PARALLEL_UPDATES = 4

  val outDir = Paths.get("./benchmarks/results/")
  if (!outDir.toFile.exists()) outDir.toFile.mkdirs()
  val csvFile = new PrintWriter(Files.newOutputStream(outDir.resolve("state_size_benchmark.csv")))
  val csvHeader = "concurrentUpdates,commonElements,uniqueElements,untrustedReplicaSize,mergedSize"
  println(csvHeader)
  csvFile.println(csvHeader)
  val dummyKeyValuePairs = Helper.dummyKeyValuePairs(MAX_TESTED_ELEMENTS)
  val aead = Helper.setupAead("AES128_GCM")

  val minElementExponent = 4 // 10 ** this as minimum tested total elements added to CRDT
  val maxElementExponent = 4 // 10 ** this as maximum tested total elements added to CRDT
  for (totalElements <- (minElementExponent to maxElementExponent).map(i => math.pow(10, i).toInt)) {
    val crdt = new DeltaAddWinsLastWriterWinsMap[String, String]("0")
    var versionVector: VectorClock = VectorClock()

    for (i <- 0 until totalElements - MAX_PARALLEL_UPDATES) {
      val entry = dummyKeyValuePairs(i)
      crdt.put(entry._1, entry._2)
      versionVector = versionVector.advance("0")
    }

    for (parallelUpdates <- 1 to MAX_PARALLEL_UPDATES) {
      for (i <- (totalElements - MAX_PARALLEL_UPDATES) to (totalElements - parallelUpdates)) {
        val entry = dummyKeyValuePairs(i)
        crdt.put(entry._1, entry._2)
        versionVector = versionVector.advance("0")
      }

      val commonState = crdt.state
      val commonStateDec = DecryptedState(commonState, versionVector)
      val commonStateEnc = commonStateDec.encrypt(Helper.setupAead("AES128_GCM"))
      val untrustedReplica = new UntrustedStateBasedReplicaMock(Set(commonStateEnc))

      var decryptedStatesMerged: DecryptedState[StateType[String, String]] = commonStateDec

      for (replicaId <- 1 to parallelUpdates) {
        val entry = dummyKeyValuePairs(totalElements - parallelUpdates + replicaId - 1)
        val replicaSpecificCrdt = new DeltaAddWinsLastWriterWinsMap[String, String](replicaId.toString, commonState)
        replicaSpecificCrdt.put(entry._1, entry._2)
        val replicaSpecificVersionVector = versionVector.advance(replicaId.toString)
        val replicaSpecificDecState: DecryptedState[StateType[String, String]] = DecryptedState(replicaSpecificCrdt.state, replicaSpecificVersionVector)
        val replicaSpecificEncState = replicaSpecificDecState.encrypt(aead)
        untrustedReplica.receive(replicaSpecificEncState)
        decryptedStatesMerged = DecryptedState.lattice[StateType[String, String]].merged(decryptedStatesMerged, replicaSpecificDecState)
      }

      val mergedSize = writeToArray(decryptedStatesMerged.state).length
      val csvLine = s"$parallelUpdates,${totalElements - parallelUpdates},$totalElements,${untrustedReplica.size},$mergedSize"
      println(csvLine)
      csvFile.println(csvLine)
    }
  }

  csvFile.close()
}

class UntrustedStateBasedReplicaMock(encryptedStates: Set[EncryptedState]) extends UntrustedReplica(encryptedStates) {
  override protected def disseminate(encryptedState: EncryptedState): Unit = {}

  def size: Int = {
    stateStore.toList.map { encState =>
      encState.serialVersionVector.length + encState.stateCiphertext.length
    }.sum
  }
}
