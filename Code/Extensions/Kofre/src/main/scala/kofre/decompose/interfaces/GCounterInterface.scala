package kofre.decompose.interfaces

import kofre.decompose.*
import kofre.syntax.{DeltaMutator, DeltaQuery}
import kofre.decompose.UIJDLattice.*

object GCounterInterface {
  type State = Map[String, Int]

  trait GCounterCompanion {
    type State = GCounterInterface.State
  }

  implicit val StateUIJDLattice: UIJDLattice[State] = MapAsUIJDLattice[String, Int]

  def value: DeltaQuery[State, Int] = state => state.values.sum

  def inc(): DeltaMutator[State] = (replicaID, state) => Map(replicaID -> (state.getOrElse(replicaID, 0) + 1))
}

/** A GCounter is a Delta CRDT modeling an increment-only counter. */
abstract class GCounterInterface[Wrapper] extends CRDTInterface[GCounterInterface.State, Wrapper] {
  def value: Int = query(GCounterInterface.value)

  def inc(): Wrapper = mutate(GCounterInterface.inc())
}
