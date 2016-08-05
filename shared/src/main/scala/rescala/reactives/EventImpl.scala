package rescala.reactives

import rescala.engines.Ticket
import rescala.graph.{PulseOption, Reactive, Struct}

trait EventImpl[+T, S <: Struct] extends Event[T, S, SignalImpl, EventImpl] with PulseOption[T, S]{

  /** add an observer */
  final override def observe(react: T => Unit)(implicit ticket: Ticket[S]): Observe[S] = Observe(this)(react)

  /**
    * Events disjunction.
    */
  final override def ||[U >: T](other: EventImpl[U, S])(implicit ticket: Ticket[S]) : EventImpl[U, S] = Events.or(this, other)

  /**
    * Event filtered with a predicate
    */
  final override def &&(pred: T => Boolean)(implicit ticket: Ticket[S]): EventImpl[T, S] = Events.filter(this)(pred)

  /**
    * Event is triggered except if the other one is triggered
    */
  final override def \[U](other: EventImpl[U, S])(implicit ticket: Ticket[S]): EventImpl[T, S] = Events.except(this, other)

  /**
    * Events conjunction
    */
  final override def and[U, R](other: EventImpl[U, S])(merger: (T, U) => R)(implicit ticket: Ticket[S]): EventImpl[R, S] = Events.and(this, other)(merger)

  /**
    * Event conjunction with a merge method creating a tuple of both event parameters
    */
  final override def zip[U](other: EventImpl[U, S])(implicit ticket: Ticket[S]): EventImpl[(T, U), S] = Events.and(this, other)((_, _))

  /**
    * Transform the event parameter
    */
  final override def map[U](mapping: T => U)(implicit ticket: Ticket[S]): EventImpl[U, S] = Events.map(this)(mapping)


  /** folds events with a given fold function to create a Signal */
  final override def fold[A](init: A)(fold: (A, T) => A)(implicit ticket: Ticket[S]) = Signals.fold(this, init)(fold)

  /** Switch back and forth between two signals on occurrence of event e */
  final override def toggle[A](a: SignalImpl[A, S], b: SignalImpl[A, S])(implicit ticket: Ticket[S]): SignalImpl[A, S] = ticket { turn =>
    val switched: SignalImpl[Boolean, S] = iterate(false) { !_ }(turn)
    Signals.dynamic(switched, a, b) { s => if (switched(s)) b(s) else a(s) }(turn)
  }

  /** Return a Signal that is updated only when e fires, and has the value of the signal s */
  final override def snapshot[A](s: SignalImpl[A, S])(implicit ticket: Ticket[S]): SignalImpl[A, S] = ticket { turn =>
    Signals.Impl.makeStatic(Set[Reactive[S]](this, s), s.get(turn))((t, current) => this.pulse(t).fold(current, _ => s.get(t)))(turn)
  }

  /** Switch to a new Signal once, on the occurrence of event e. */
  final override def switchOnce[A](original: SignalImpl[A, S], newSignal: SignalImpl[A, S])(implicit ticket: Ticket[S]): SignalImpl[A, S] = ticket { implicit turn =>
    val latest = latestOption
    Signals.dynamic(latest, original, newSignal) { t =>
      latest(t) match {
        case None => original(t)
        case Some(_) => newSignal(t)
      }
    }
  }

  /**
    * Switch to a signal once, on the occurrence of event e. Initially the
    * return value is set to the original signal. When the event fires,
    * the result is a constant signal whose value is the value of the event.
    */
  final override def switchTo[T1 >: T](original: SignalImpl[T1, S])(implicit ticket: Ticket[S]): SignalImpl[T1, S] = {
    val latest = latestOption
    Signals.dynamic(latest, original) { s =>
      latest(s) match {
        case None => original(s)
        case Some(x) => x
      }
    }
  }

  /** returns the values produced by the last event produced by mapping this value */
  final override def flatMap[B](f: T => EventImpl[B, S])(implicit ticket: Ticket[S]): EventImpl[B, S] = ticket { implicit t =>
    Events.wrapped(map(f).latest(Evt()))
  }
}

