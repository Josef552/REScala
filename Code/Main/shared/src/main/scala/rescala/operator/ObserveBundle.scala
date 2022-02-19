package rescala.operator

import rescala.core.Core

trait ObserveBundle extends Core {

  trait ObserveInteract extends Observation {
    // if true, the observer will remove all of its inputs, which allows eventual collection
    def checkExceptionAndRemoval(): Boolean
  }

  /** observers are normale reactives that are configured by a function that converts the value of the input into an [[ObserveInteract]]*/
  object Observe {
    def strong[T](
        dependency: ReSource,
        fireImmediately: Boolean
    )(fun: dependency.Value => ObserveInteract)(implicit ct: CreationTicket): Disconnectable = {
      ct.create[Pulse[Nothing], Disconnectable with Derived](Set(dependency), Pulse.NoChange, fireImmediately) { state =>
        class Obs extends Base[Pulse[Nothing]](state, ct.rename) with Derived with DisconnectableImpl {

          override protected[rescala] def commit(base: Obs.this.Value): Obs.this.Value = Pulse.NoChange

          override protected[rescala] def guardedReevaluate(dt: ReIn): Rout = {
            val v  = dt.collectStatic(dependency)
            val oi = fun(v)
            if (oi.checkExceptionAndRemoval()) dt.trackDependencies(Set.empty)
            else dt.withEffect(oi)
          }
        }
        new Obs
      }
    }

  }
}
