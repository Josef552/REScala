package rescala.crdts.pvars

import rescala.crdts.pvars.Publishable.PVarFactory
import rescala.default._
import rescala.crdts.statecrdts.sequences.{RGOA, Vertex}

case class PGrowOnlyLog[A](initial: RGOA[A] = RGOA[A](),
                           internalChanges: Evt[RGOA[A]] = Evt[RGOA[A]],
                           externalChanges: Evt[RGOA[A]] = Evt[RGOA[A]]) extends Publishable[List[A], RGOA[A]]()(RGOA.RGOAStateCRDTInstance[A]) {

  def append(a: A): Unit = internalChanges.fire(crdtSignal.readValueOnce.append(Vertex(a)))

  def contains(a: A): Boolean = crdtSignal.readValueOnce.containsValue(a)

  // allows the log to log events of type a and append them to the log
  def observe(e: Event[A]):Unit = e.observe(a => append(a))
}

object PGrowOnlyLog {
  /**
    * Allows creation of PVertexLogs by passing a set of initial values.
    */
  def apply[A](values: List[A]): PGrowOnlyLog[A] = {
    val init: RGOA[A] = RGOA().fromValue(values)
    new PGrowOnlyLog[A](init)
  }

  implicit def PGrowOnlyLogFactory[A]: PVarFactory[PGrowOnlyLog[A]] =
    new PVarFactory[PGrowOnlyLog[A]] {
      override def apply(): PGrowOnlyLog[A] = PGrowOnlyLog[A]()
    }
}
