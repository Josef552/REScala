package rescala.fullmv.mirrors

import rescala.core.Derived
import rescala.core._
import rescala.fullmv._

import scala.concurrent.duration.Duration

object ReactiveMirror {
  def apply[A](reactive: ReSource[FullMVStruct], turn: FullMVTurn, reflectionIsTransient: Boolean, rename: ReName)(
      toPulse: reactive.Value => A,
      reflectionProxy: ReactiveReflectionProxy[A]
  ): (List[(FullMVTurn, A)], Option[FullMVTurn]) = {
    assert(
      turn.host == reactive.state.host,
      s"mirror installation for $reactive on ${reactive.state.host} with $turn from different ${turn.host}"
    )
    def getValue(turn: FullMVTurn): A = toPulse(reactive.state.staticAfter(turn))
    val mirror                        = new ReactiveMirror(getValue, reflectionProxy, turn.host.timeout, rename)

    val ownValue                                    = toPulse(reactive.state.dynamicAfter(turn))
    val (successorWrittenVersions, maybeFirstFrame) = reactive.state.discover(turn, mirror)
    val mustAddBaseValue =
      !reflectionIsTransient && (successorWrittenVersions.isEmpty || successorWrittenVersions.head != turn)
    val initValues = successorWrittenVersions.map { succ =>
      succ -> getValue(succ)
    }
    (if (mustAddBaseValue) ((turn, ownValue) :: initValues) else initValues, maybeFirstFrame)
  }
}

class ReactiveMirror[A](
    val getValue: FullMVTurn => A,
    val reflectionProxy: ReactiveReflectionProxy[A],
    val timeout: Duration,
    override val name: ReName
) extends Derived[FullMVStruct]
    with FullMVState[Nothing, FullMVTurn, ReSource[FullMVStruct], Derived[FullMVStruct]] {
  override type Value = Nothing
  override protected[rescala] val state = this
  override def toString: String         = s"Mirror${name.str}"
  override val host: FullMVEngine       = null
  override def incrementFrame(txn: FullMVTurn): FramingBranchResult[FullMVTurn, Derived[FullMVStruct]] = {
    FullMVEngine.myAwait(txn.addRemoteBranch(TurnPhase.Framing), timeout)
    reflectionProxy.asyncIncrementFrame(txn)
    FramingBranchResult.FramingBranchEnd
  }
  override def incrementSupersedeFrame(
      txn: FullMVTurn,
      supersede: FullMVTurn
  ): FramingBranchResult[FullMVTurn, Derived[FullMVStruct]] = {
    FullMVEngine.myAwait(txn.addRemoteBranch(TurnPhase.Framing), timeout)
    reflectionProxy.asyncIncrementSupersedeFrame(txn, supersede)
    FramingBranchResult.FramingBranchEnd
  }
  override def notify(
      txn: FullMVTurn,
      changed: Boolean
  ): (Boolean, NotificationBranchResult[FullMVTurn, Derived[FullMVStruct]]) = {
    FullMVEngine.myAwait(txn.addRemoteBranch(TurnPhase.Executing), timeout)
    if (changed) {
      reflectionProxy.asyncNewValue(txn, getValue(txn))
    } else {
      reflectionProxy.asyncResolvedUnchanged(txn)
    }
    false -> NotificationBranchResult.DoNothing
  }
  override def notifyFollowFrame(
      txn: FullMVTurn,
      changed: Boolean,
      followFrame: FullMVTurn
  ): (Boolean, NotificationBranchResult[FullMVTurn, Derived[FullMVStruct]]) = {
    FullMVEngine.myAwait(txn.addRemoteBranch(TurnPhase.Executing), timeout)
    if (changed) {
      reflectionProxy.asyncNewValueFollowFrame(txn, getValue(txn), followFrame)
    } else {
      reflectionProxy.asyncResolvedUnchangedFollowFrame(txn, followFrame)
    }
    false -> NotificationBranchResult.DoNothing
  }
  override def latestValue: Value                = ???
  override def reevIn(turn: FullMVTurn): Nothing = ???
  override def reevOut(
      turn: FullMVTurn,
      maybeValue: Option[Value]
  ): NotificationBranchResult.ReevOutBranchResult[FullMVTurn, Derived[FullMVStruct]]                         = ???
  override def dynamicBefore(txn: FullMVTurn): Nothing                                                       = ???
  override def staticBefore(txn: FullMVTurn): Nothing                                                        = ???
  override def dynamicAfter(txn: FullMVTurn): Nothing                                                        = ???
  override def staticAfter(txn: FullMVTurn): Nothing                                                         = ???
  override def discover(txn: FullMVTurn, add: Derived[FullMVStruct]): (List[FullMVTurn], Option[FullMVTurn]) = ???
  override def drop(txn: FullMVTurn, remove: Derived[FullMVStruct]): (List[FullMVTurn], Option[FullMVTurn])  = ???
  override def retrofitSinkFrames(
      successorWrittenVersions: Seq[FullMVTurn],
      maybeSuccessorFrame: Option[FullMVTurn],
      arity: Int
  ): Seq[FullMVTurn] = ???

  override protected[rescala] def reevaluate(input: ReIn) = ???
}
