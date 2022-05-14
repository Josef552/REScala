package kofre.datatypes

import kofre.base.{Bottom, DecomposeLattice}
import kofre.decompose.*
import kofre.dotted.DottedDecompose
import kofre.syntax.OpsSyntaxHelper

/** A TwoPhaseSet (Two-Phase Set) is a Delta CRDT modeling a set.
  *
  * The set is modeled as two grow-only sets, a set of added elements and a set of removed elements. Because of this,
  * elements that were removed from the set once can never be re-added.
  */

case class TwoPhaseSet[E](added: Set[E], removed: Set[E])

object TwoPhaseSet {
  def empty[E]: TwoPhaseSet[E] = TwoPhaseSet(Set.empty, Set.empty)

  given bottom[E]: Bottom[TwoPhaseSet[E]] with { override def empty: TwoPhaseSet[E] = TwoPhaseSet.empty }

  given decomposeLattice[E]: DecomposeLattice[TwoPhaseSet[E]] = DecomposeLattice.derived
  given contextDecompose[E]: DottedDecompose[TwoPhaseSet[E]]  = DottedDecompose.liftDecomposeLattice

  implicit class TwoPSetOps[C, E](container: C) extends OpsSyntaxHelper[C, TwoPhaseSet[E]](container) {

    def elements(using QueryP): Set[E] = {
      current.added diff current.removed
    }

    def contains(element: E)(using QueryP): Boolean = current.added.contains(element) && !current.removed.contains(element)

    def insert(element: E)(using MutationP): C = TwoPhaseSet(Set(element), Set.empty).mutator

    def remove(element: E)(using MutationP): C = TwoPhaseSet(Set.empty, Set(element)).mutator
    def removeAll(elements: Set[E])(using MutationP): C = TwoPhaseSet(Set.empty, elements).mutator
  }
}
