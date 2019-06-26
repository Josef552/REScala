package rescala.incremental

import rescala.core._
import rescala.macros.cutOutOfUserComputation
import rescala.reactives.Signal



trait Delta[T]
case class Addition[T](value: T) extends Delta[T]
case class Removal[T](value: T) extends Delta[T]



trait ReactiveDeltaSet[T, S <: Struct] extends ReSource[S] {

  /** the value of deltas send through the set */
  override type Value = Delta[T]


  @cutOutOfUserComputation
  def map[A](expression: T => A)(implicit ticket: CreationTicket[S]): ReactiveDeltaSet[A, S] = ???

  @cutOutOfUserComputation
  def filter[A](expression: T => Boolean)(implicit ticket: CreationTicket[S]): ReactiveDeltaSet[A, S] = ???

  @cutOutOfUserComputation
  def aggregate[A](expression: (A, Delta[T]) => A)(implicit ticket: CreationTicket[S]): Signal[A, S] = ???

}



/** Source events with imperative occurrences
  *
  * @param initialState of by the event
  * @tparam T Type returned when the event fires
  * @tparam S Struct type used for the propagation of the event
  */
final class SetSource[T, S <: Struct] private[rescala](initialState:  S#State[Delta[T], S], name: REName)
  extends Base[Delta[T], S](initialState, name) with ReactiveDeltaSet[T, S] {

  def add(value: T)(implicit fac: Scheduler[S]): Unit =
    fac.forceNewTransaction(this) {
      addInTx(Addition(value))(_)
    }

  def addInTx(delta: Delta[T])(implicit ticket: AdmissionTicket[S]): Unit = {
    ticket.recordChange(new InitialChange[S] {
      override val source = SetSource.this
      override def writeValue(b: Delta[T], v: Delta[T] => Unit): Boolean = {v(delta); true}
    })
  }
}

