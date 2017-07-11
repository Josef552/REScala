package rescala.core

import rescala.util.{Globals, REName}

import scala.language.higherKinds


trait Struct { type State[P, S <: Struct] }

/**
  * A reactive value is something that can be reevaluated
  *
  * @tparam S Struct type that defines the spore type used to manage the reactive evaluation
  */
trait Reactive[S <: Struct] {

  type Value
  final override val hashCode: Int = Globals.nextID().hashCode()

  /**
    * Spore that is used to internally manage the reactive evaluation of this value
    *
    * @return Spore for this value
    */
  protected[rescala] def state: S#State[Value, S]

  protected[rescala] def reevaluate(turn: Turn[S], before: Value, indeps: Set[Reactive[S]]): ReevaluationResult[Value, S]

}

/**
  * A pulsing value is a reactive value that stores a pulse with it's old and new value
  *
  * @tparam P Value type stored by the pulse of the reactive value
  * @tparam S Struct type that defines the spore type used to manage the reactive evaluation
  */
trait Pulsing[+P, S <: Struct] extends Reactive[S] {
  override type Value <: P
}


/** helper class implementing the state methods of reactive and pulsing */
abstract class Base[P, S <: Struct](initialState: S#State[Pulse[P], S], rename: REName) extends Pulsing[Pulse[P], S] {
  override type Value = Pulse[P]
  override def toString: String = rename.name
  final override protected[rescala] def state: S#State[Value, S] = initialState
}




