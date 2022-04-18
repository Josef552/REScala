package rescala.extra.encrdt.encrypted.statebased

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.google.crypto.tink.Aead
import kofre.causality.VectorClock
import kofre.Lattice.Operators

abstract class TrustedReplica[T](val localReplicaId: String,
                                 private val aead: Aead)
                                (implicit val stateJsonCodec: JsonValueCodec[T]) extends Replica {

  var versionVector: VectorClock = VectorClock.zero

  def receive(encryptedState: EncryptedState): Unit = {
    val decryptedState: DecryptedState[T] = encryptedState.decrypt(aead)
    versionVector = versionVector.merge(decryptedState.versionVector)
    // TODO: synchronize
    merge(decryptedState.state)
  }

  def stateChanged(state: T): Unit = {
    versionVector = versionVector.inc(localReplicaId)
    val encryptedState = DecryptedState(state, versionVector).encrypt(aead)
    disseminate(encryptedState)
  }

  protected def merge(state: T): Unit

  protected def localState(): T

}
