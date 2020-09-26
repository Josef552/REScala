package rescala.twoversion

import rescala.core.{AdmissionTicket, Initializer, ReSource, DynamicInitializerLookup}
import rescala.reactives.Signal

/**
  * Implementation of the turn handling defined in the Engine trait
  *
  * @tparam S Struct type that defines the spore type used to manage the reactive evaluation
  * @tparam Tx Transaction type used by the scheduler
  */
trait TwoVersionScheduler[S <: TwoVersionStruct, Tx <: TwoVersionTransaction[S] with Initializer[S]]
    extends DynamicInitializerLookup[S, Tx] {
  override private[rescala] def singleReadValueOnce[A](reactive: Signal[A, S]): A =
    reactive.resource.interpret(reactive.resource.state.base(null))

  /** goes through the whole turn lifecycle
    * - create a new turn and put it on the stack
    * - run the lock phase
    *   - the turn knows which reactives will be affected and can do something before anything is really done
    * - run the admission phase
    *   - executes the user defined admission code
    * - run the propagation phase
    *   - calculate the actual new value of the reactive graph
    * - run the commit phase
    *   - do cleanups on the reactives, make values permanent and so on, the turn is still valid during this phase
    * - run the observer phase
    *   - run registered observers, the turn is no longer valid but the locks are still held.
    * - run the release phase
    *   - this must always run, even in the case that something above fails. it should do cleanup and free any locks to avoid starvation.
    * - run the party! phase
    *   - not yet implemented
    */
  override def forceNewTransaction[R](initialWrites: Set[ReSource[S]], admissionPhase: AdmissionTicket[S] => R): R = {
    val turn = makeTurn(_currentTurn.value)

    val result =
      try {
        turn.preparationPhase(initialWrites)
        val result = withDynamicInitializer(turn) {
          val admissionTicket = turn.makeAdmissionPhaseTicket(initialWrites)
          val admissionResult = admissionPhase(admissionTicket)
          turn.initializationPhase(admissionTicket.initialChanges)
          turn.propagationPhase()
          if (admissionTicket.wrapUp != null) admissionTicket.wrapUp(turn.accessTicket())
          admissionResult
        }
        turn.commitPhase()
        result
      } catch {
        case e: Throwable =>
          turn.rollbackPhase()
          throw e
      } finally {
        turn.releasePhase()
      }
    turn.observerPhase()
    result
  }

  protected def makeTurn(priorTurn: Option[Tx]): Tx

}
