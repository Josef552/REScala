package rescala.deltacrdts

import rescala.deltacrdts.GCounter.Delta
import rescala.lattices.IdUtil.Id
import rescala.lattices.{IdUtil, Lattice}
import scala.collection.immutable.HashMap

case class GCounter(id: IdUtil.Id, payload: HashMap[IdUtil.Id, Int]) {

  def value: Int = payload.values.sum
  def increase = GCounter(id, payload + (id -> (payload(id) + 1)))


  def increaseΔ: Delta = HashMap(id -> (payload(id) + 1))
}

object GCounter {
  type Delta = HashMap[Id, Int]
  def apply(value: Int): GCounter = {
    val id = IdUtil.genId // assign random id based on host
    GCounter(id, HashMap(id -> value))
  }

  implicit def lattice: Lattice[GCounter] = new Lattice[GCounter] {
    override def merge(left: GCounter, right: GCounter): GCounter =
      GCounter(left.id,
        left.payload.merged(right.payload) {
          case ((k, v1), (_, v2)) => (k, v1 max v2)
        })
  }

  implicit def deltaCrdt: DeltaCRDT[GCounter, Delta]  = new DeltaCRDT[GCounter,Delta] {
    override def applyΔ(crdt: GCounter, delta: Delta): GCounter = GCounter(crdt.id, crdt.payload.merged(delta){
      case ((k, v1), (_, v2)) => (k, v1 max v2)
    })
  }

}
