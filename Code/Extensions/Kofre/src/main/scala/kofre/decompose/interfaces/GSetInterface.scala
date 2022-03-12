package kofre.decompose.interfaces

import kofre.decompose.*
import kofre.syntax.{DeltaMutator, DeltaQuery}

object GSetInterface {
  type State[E] = Set[E]

  trait GSetCompanion {
    type State[E] = GSetInterface.State[E]
  }

  def elements[E]: DeltaQuery[State[E], Set[E]] = state => state

  def insert[E](element: E): DeltaMutator[State[E]] = (_, _) => Set(element)
}

/** A GSet is a Delta CRDT modeling a simple grow-only set. */
abstract class GSetInterface[E, Wrapper] extends CRDTInterface[GSetInterface.State[E], Wrapper] {
  def elements: Set[E] = query(GSetInterface.elements)

  def insert(element: E): Wrapper = mutate(GSetInterface.insert(element))
}
