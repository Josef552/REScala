package kofre.encrdt.crdts.interfaces

trait Crdt[T] {
  def merge(state: T): Unit
}