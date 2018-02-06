package rescala.fullmv.mirrors.localcloning

import rescala.core._
import rescala.fullmv.mirrors.{ReactiveMirror, ReactiveReflectionImpl, ReactiveReflectionProxy}
import rescala.fullmv.{FullMVEngine, FullMVStruct, FullMVTurn}
import rescala.reactives.{Event, Signal}

object ReactiveLocalClone {
  def apply[A](signal: Signal[A, FullMVStruct], host: FullMVEngine)(implicit name: REName): Signal[A, FullMVStruct] = apply(signal)(CreationTicket.fromEngine(host)(name))
  def apply[A](signal: Signal[A, FullMVStruct])(implicit ticket: CreationTicket[FullMVStruct]): Signal[A, FullMVStruct] = {
    val valuePersistency = Initializer.DerivedSignal[A]
    ticket { creation =>
      val turn = creation /* fuckit */ .asInstanceOf[FullMVTurn]
      creation.create(Set(), valuePersistency) { initialState =>
        val reflection = new ReactiveReflectionImpl[A](turn.host, None, initialState, ticket.rename.derive("SignalReflection")) with Signal[A, FullMVStruct] {
          override def disconnect()(implicit engine: Scheduler[FullMVStruct]): Unit = ???
        }
        connectAndInitializeLocalPushClone(signal, turn, reflection, valuePersistency.isTransient, ticket.rename.name)
        reflection
      }
    }
  }

  def apply[P](event: Event[P, FullMVStruct], host: FullMVEngine)(implicit name: REName): Event[P, FullMVStruct] = apply(event)(CreationTicket.fromEngine(host)(name))
  def apply[P](event: Event[P, FullMVStruct])(implicit ticket: CreationTicket[FullMVStruct]): Event[P, FullMVStruct] = {
    val valuePersistency = Initializer.Event[P]
    ticket { creation =>
      creation.create(Set(), valuePersistency) { initialState =>
        val turn = creation /* fuckit */ .asInstanceOf[FullMVTurn]
        val reflection = new ReactiveReflectionImpl[P](turn.host, Some(turn), initialState, ticket.rename.derive("EventReflection")) with Event[P, FullMVStruct] {
          override def disconnect()(implicit engine: Scheduler[FullMVStruct]): Unit = ???
        }
        connectAndInitializeLocalPushClone(event, turn, reflection, valuePersistency.isTransient, ticket.rename.name)
        reflection
      }
    }
  }

  def connectAndInitializeLocalPushClone[A](reactive: ReSourciV[Pulse[A], FullMVStruct], connectTurn: FullMVTurn, reflection: ReactiveReflectionImpl[A], reflectionIsTransient: Boolean, rename: REName): Unit = {
    if (FullMVEngine.DEBUG) println(s"[${Thread.currentThread().getName}] $connectTurn creating clone of $reactive")
    val reflectionHost = connectTurn.host
    // simple remote interface for transfer in one direction
    val reflectionProxy: ReactiveReflectionProxy[Pulse[A]] = new ReactiveReflectionProxy[Pulse[A]] {
      override def asyncIncrementFrame(turn: FullMVTurn): Unit = reflection.asyncIncrementFrame(FullMVTurnLocalClone.active(turn, reflectionHost))
      override def asyncDecrementFrame(turn: FullMVTurn): Unit = reflection.asyncDecrementFrame(FullMVTurnLocalClone.active(turn, reflectionHost))
      override def asyncIncrementSupersedeFrame(turn: FullMVTurn, supersede: FullMVTurn): Unit = reflection.asyncIncrementSupersedeFrame(FullMVTurnLocalClone.active(turn, reflectionHost), FullMVTurnLocalClone.active(supersede, reflectionHost))
      override def asyncDeframeReframe(turn: FullMVTurn, reframe: FullMVTurn): Unit = reflection.asyncDeframeReframe(FullMVTurnLocalClone.active(turn, reflectionHost), FullMVTurnLocalClone.active(reframe, reflectionHost))
      override def asyncNewValue(turn: FullMVTurn, value: Pulse[A]): Unit = reflection.asyncNewValue(FullMVTurnLocalClone.active(turn, reflectionHost), value)
      override def asyncResolvedUnchanged(turn: FullMVTurn): Unit = reflection.asyncResolvedUnchanged(FullMVTurnLocalClone.active(turn, reflectionHost))
      override def asyncResolvedUnchangedFollowFrame(turn: FullMVTurn, followFrame: FullMVTurn): Unit = reflection.asyncResolvedUnchangedFollowFrame(FullMVTurnLocalClone.active(turn, reflectionHost), FullMVTurnLocalClone.active(followFrame, reflectionHost))
      override def asyncNewValueFollowFrame(turn: FullMVTurn, value: Pulse[A], followFrame: FullMVTurn): Unit = reflection.asyncNewValueFollowFrame(FullMVTurnLocalClone.active(turn, reflectionHost), value, FullMVTurnLocalClone.active(followFrame, reflectionHost))
    }
    // simple initialization data for transfer in the other direction
    val (mirrorInitValues, mirrorMaybeFirstFrame) = ReactiveMirror(reactive, FullMVTurnLocalClone.active(connectTurn, reactive.state.host), reflectionProxy, reflectionIsTransient, rename.derive("Mirror"))

    val reflectionInitValues = mirrorInitValues.map{ case (mirrorTurn, value) => FullMVTurnLocalClone.active(mirrorTurn, reflectionHost) -> value }
    val reflectionMaybeFirstFrame = mirrorMaybeFirstFrame.map(FullMVTurnLocalClone.active(_, reflectionHost))

    reflection.state.retrofitSinkFrames(reflectionInitValues.map(_._1), reflectionMaybeFirstFrame, +1)
    for((reflectionTurn, value) <- reflectionInitValues) reflection.buffer(reflectionTurn, value)
  }
}
