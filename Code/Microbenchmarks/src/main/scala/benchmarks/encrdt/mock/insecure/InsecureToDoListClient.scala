package benchmarks.encrdt.mock.insecure

import benchmarks.encrdt.mock.SecureToDoListClient.ToDoMapLattice
import benchmarks.encrdt.mock.{SecureToDoListClient, ToDoListIntermediary}
import benchmarks.encrdt.todolist.ToDoEntry
import com.github.plokhotnyuk.jsoniter_scala.core.writeToArray
import kofre.encrdt.crdts.DeltaAddWinsLastWriterWinsMap
import rescala.extra.encrdt.encrypted.deltabased.{DecryptedDeltaGroup, EncryptedDeltaGroup}

import java.util.UUID

class InsecureToDoListClient(
    replicaId: String,
    crdt: DeltaAddWinsLastWriterWinsMap[UUID, ToDoEntry],
    untrustedReplica: ToDoListIntermediary
) extends SecureToDoListClient(replicaId, crdt, null, untrustedReplica) {
  override protected def encryptAndDisseminate(newDeltaGroup: DecryptedDeltaGroup[ToDoMapLattice]): Unit = {
    // Serialize but don't encrypt!
    val serialPlaintextDeltaGroup = writeToArray(newDeltaGroup.deltaGroup)
    val serialDottedVersionVector = writeToArray(newDeltaGroup.dottedVersionVector)

    disseminate(
      EncryptedDeltaGroup(serialPlaintextDeltaGroup, serialDottedVersionVector)
    )
  }
}
