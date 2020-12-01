package rescala.extra.reactivestreams

import java.util.Objects

import org.reactivestreams.{Subscriber, Subscription}
import rescala.core.{Scheduler, Struct}
import rescala.operator.{Evt, Pulse}

class RESubscriber[T, S <: Struct](evt: Evt[T, S], fac: Scheduler[S]) extends Subscriber[T] {

  var subscription: Subscription = _

  override def onError(thrw: Throwable): Unit =
    synchronized {
      Objects.requireNonNull(thrw)
      fac.forceNewTransaction(evt) { implicit turn => evt.admitPulse(Pulse.Exceptional(thrw)) }
    }
  override def onSubscribe(s: Subscription): Unit =
    synchronized {
      subscription = s
      subscription.request(1)
    }
  override def onComplete(): Unit = {}
  override def onNext(value: T): Unit =
    synchronized {
      Objects.requireNonNull(value)
      evt.fire(value)(fac)
      subscription.request(1)
    }
}
