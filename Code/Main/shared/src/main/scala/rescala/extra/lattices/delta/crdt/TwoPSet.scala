package rescala.extra.lattices.delta.crdt

import rescala.extra.lattices.delta.{Delta, DeltaCRDT}
import rescala.extra.lattices.delta.DeltaCRDT.{DeltaMutator, DeltaQuery}
import rescala.extra.lattices.delta.UIJDLatticeWithBottom.PairAsUIJDLattice


object TwoPSet {
  type State[E] = (Set[E], Set[E])

  def apply[E](replicaID: String): DeltaCRDT[State[E]] =
    DeltaCRDT.empty[State[E]](replicaID)

  def elements[E]: DeltaQuery[State[E], Set[E]] = {
    case (add, remove) => add diff remove
  }

  def insert[E](element: E): DeltaMutator[State[E]] = (replicaID, _) =>
    Delta(
      replicaID,
      (Set(element), Set.empty[E])
    )

  def remove[E](element: E): DeltaMutator[State[E]] = (replicaID, _) =>
    Delta(
      replicaID,
      (Set.empty[E], Set(element))
    )
}
