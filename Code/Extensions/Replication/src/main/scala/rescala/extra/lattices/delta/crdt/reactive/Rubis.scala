package rescala.extra.lattices.delta.crdt.reactive

import rescala.extra.lattices.delta.{CContext, Delta, UIJDLattice}
import rescala.extra.lattices.delta.interfaces.RubisInterface
import rescala.extra.lattices.delta.interfaces.RubisInterface.{RubisCompanion, State}

class Rubis[C: CContext](
    protected[rescala] val state: State[C],
    protected val replicaID: String,
    protected[rescala] val deltaBuffer: List[Delta[State[C]]]
) extends RubisInterface[C, Rubis[C]] with ReactiveCRDT[State[C], Rubis[C]] {

  override protected def copy(state: State[C], deltaBuffer: List[Delta[State[C]]]): Rubis[C] =
    new Rubis(state, replicaID, deltaBuffer)
}

object Rubis extends RubisCompanion {
  def apply[C: CContext](replicaID: String): Rubis[C] =
    new Rubis(UIJDLattice[State[C]].bottom, replicaID, List())
}