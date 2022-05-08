package kofre.encrdt.lattices

import kofre.base.{DecomposeLattice, Lattice}
import kofre.causality.CausalContext
import kofre.contextual.WithContext
import kofre.predef.AddWinsSet
import kofre.syntax.{PermIdMutate, WithNamedContext}
import kofre.syntax.PermIdMutate.withID

case class AddWinsMapLattice[K, V](
    keys: WithContext[AddWinsSet[K]] = WithContext.empty[AddWinsSet[K]],
    mappings: Map[K, V] = Map[K, V]()
) {
  def values: Map[K, V] = mappings

  def added(key: K, value: V, replicaId: String): AddWinsMapLattice[K, V] = {
    val newKeys = keys merged keys.named(replicaId).add(key).inner
    val newMap  = mappings + (key -> value)
    AddWinsMapLattice(newKeys, newMap)
  }

  def removed(key: K): AddWinsMapLattice[K, V] = {
    val newKeys = keys merged keys.remove(key)
    val newMap  = mappings - key
    AddWinsMapLattice(newKeys, newMap)
  }
}

object AddWinsMapLattice {
  given AddWinsLattice[K, V: Lattice]: Lattice[AddWinsMapLattice[K, V]] = Lattice.derived
}
