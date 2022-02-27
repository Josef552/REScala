package kofre.primitives

import kofre.Defs.Id
import kofre.Lattice
import kofre.Lattice.Operators
import kofre.causality.VectorClock

import scala.annotation.tailrec

/** Keeps all concurrent writes */
case class MultiValueRegister[T](versions: Map[VectorClock, T]) {
  lazy val currentTime: VectorClock = {
    if (versions.isEmpty) VectorClock.zero
    else versions.keys.reduce((a, b) => a.merge(b))
  }

  def values: Iterable[T] = versions.values

  def write(replica: Id, value: T): MultiValueRegister[T] = {
    val timeOfUpdate = currentTime merge currentTime.inc(replica)
    MultiValueRegister(Map(timeOfUpdate -> value))
  }
}

object MultiValueRegister {
  given lattice[T]: Lattice[MultiValueRegister[T]] =
    (left, right) => {
      val both   = left.versions ++ right.versions
      val toKeep = parallelVersionSubset(both.keySet.toList, List.empty)
      MultiValueRegister(both.filter { case (k, v) => toKeep.contains(k) })
    }

  @tailrec
  def parallelVersionSubset(remaining: List[VectorClock], acc: List[VectorClock]): List[VectorClock] =
    remaining match
      case Nil       => acc
      case h :: tail =>
        // remove smaller ones from the list we operate on
        val rem = tail.filterNot(e => e <= h)
        // remove smaller ones from acc
        val nacc = acc.filterNot(e => e <= h)
        // continue
        parallelVersionSubset(rem, h :: nacc)
}
