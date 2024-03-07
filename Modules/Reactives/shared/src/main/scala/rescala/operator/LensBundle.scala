package rescala.operator

import rescala.core.*

trait LensBundle {
  self: Operators =>

  /**
   * LVars serve as the basis for reactive lenses. To create the root of a new LVar cluster, use the apply() function
   * of the LVar object. Then, connect additional LVars via Lenses using the applyLens() function of an existing LVar.
   *
   * @param state  The state of the LVar
   * @param events Incoming events indicating a change to the LVar cluster
   */
  class LVar[M] private[rescala](state: Signal[M], events: Evt[Event[M]]) {

    /**
     * TODO: The BijectiveSigLens requires a reactive read without evaluating dependencies. As this is currently not supported by REScala, it uses .now instead!
     * Creates a new LVar which is connected to this LVar via the given Lens.
     *
     * @param lens The lens which connects the LVars. Can use implicit conversion from Lens if the Lens does not need to change later
     */

    def applyLens[V](lens: SignalLens[M, V])(implicit ticket: CreationTicket[BundleState], sched: Scheduler[BundleState]): LVar[V] = {
      val newVar = new LVar[V](state.map { model => lens.toView(model) }.flatten, Evt())
      events.fire(newVar.getEvent().map { e => lens.toModel(e, this.state.now) })
      return newVar
    }

    /**
     * Register an event which should trigger a change of this LVar (and consequently the entire lens cluster)
     *
     * @param e The event which should change the LVar
     */
    def fire(e: Event[M])(implicit sched: Scheduler[BundleState]): Unit = events.fire(e)

    /**
     * Function to observe a change of the LVar. Simple wrapper for internal.observe
     */
    def observe(onValue: M => Unit, onError: Throwable => Unit = null, fireImmediately: Boolean = false)
               (implicit ticket: CreationTicket[BundleState]) = state.observe(onValue, onError, fireImmediately)

    /**
     * Returns the first firing event of all registered events
     */
    def getEvent()(implicit ticket: CreationTicket[BundleState]): Event[M] = events.list().flatten(firstFiringEvent)

    /**
     * Function to access the current value of the lens. Simple wrapper for internal.now
     */
    inline def now(implicit sched: Scheduler[BundleState]): M = state.now

    /**
     * Function to access state of LVar in reactives. Simple wrapper for internal.value.
     */
    inline def value(implicit sched: Scheduler[BundleState]): M = state.value

  }

  object LVar {

    /**
     * Creates a new LVar with the given initial value. This will be the root of a new Lens cluster
     *
     * @param initval the inital value of the LVar
     */
    def apply[T](initval: T)(implicit ticket: CreationTicket[BundleState]): LVar[T] = {
      val events: Evt[Event[T]] = Evt()
      new LVar[T](events.list().flatten(firstFiringEvent).hold(initval), events)
    }

  }

  /**
   * The base type for all lenses. If possible, use BijectiveLens instead as it provides more performance and additional functionality
   *
   * @tparam M the type of the model
   * @tparam V the type of the view
   */
  trait Lens[M, V] {

    /**
     * Transforms the model to the view
     */
    def toView(m: M): V

    /**
     * Transforms the view to the model using the old model state
     */
    def toModel(v: V, m: M): M

    /**
     * Concatenates this lens with another lens and returns the resulting lens.
     * Internally, a LensVar is created, meaning that this function is just for convenience and not performance
     *
     * @param other The other lens
     */
    def compose[W](other: Lens[V, W]): Lens[M, W] = new Lens[M, W] {
      override def toView(m: M): W = other.toView(Lens.this.toView(m))

      override def toModel(w: W, m: M): M = {
        val viewOfA = Lens.this.toView(m)
        Lens.this.toModel(other.toModel(w, viewOfA), m)
      }
    }
  }

  /**
   * The base trait for all bijective lenses
   *
   * @tparam M The type of the model
   * @tparam V The type of the view
   */
  trait BijectiveLens[M, V] extends Lens[M, V] {

    /**
     * Override the toModel function to make the lens bijective
     */
    def toModel(v: V): M

    override def toModel(v: V, m: M) = toModel(v)

    /**
     * Inverts the lens such that e.g. an AddLens functions like a SubLens. Note that this does not change the model-view relationship,
     * i.e. the asymmetry is not inverted.
     */
    def inverse: BijectiveLens[V, M] = new BijectiveLens[V, M] {
      override def toView(m: V): M = BijectiveLens.this.toModel(m)

      override def toModel(v: M): V = BijectiveLens.this.toView(v)
    }

    /**
     * Overloads the compose function to return a bijective lens
     * This version does not use an LVar internally, making it more efficient than the implementation in Lens
     *
     * @param other The other lens
     */
    def compose[W](other: BijectiveLens[V, W]): BijectiveLens[M, W] = new BijectiveLens[M, W] {
      override def toView(m: M): W = other.toView(BijectiveLens.this.toView(m))

      override def toModel(w: W): M = BijectiveLens.this.toModel(other.toModel(w))
    }
  }

  /**
   * TODO: The SignalLens requires a reactive read without evaluating dependencies. As this is currently not supported by REScala, it uses .now instead!
   *
   * @param signalOfLens A Signal of a Lens
   */
  class SignalLens[M, V](signalOfLens: Signal[Lens[M, V]])(implicit sched: Scheduler[BundleState]) {
    def toView(m: M): Signal[V] = signalOfLens.map { model => model.toView(m) }

    def toModel(v: V, m: M): M = signalOfLens.now.toModel(v, m)
  }

  /**
   * Implicit conversion of a Lens to a SignalLens for uniform handling.
   */
  implicit def toSignalLens[M, V](lens: Lens[M, V])(implicit ticket: CreationTicket[BundleState],
                                                    sched: Scheduler[BundleState]): SignalLens[M, V] = SignalLens(Signal {
    lens
  })

  /**
   * A simple lens for addition
   *
   * @param k The summand
   */
  class AddLens[A](k: A)(implicit num: Numeric[A]) extends BijectiveLens[A, A] {
    def toView(m: A): A = num.plus(m, k)

    def toModel(v: A): A = num.minus(v, k)
  }

  /**
   * A simple lens for multiplication
   *
   * @param k The summand
   */
  class MulLens[A](k: A)(implicit frac: Fractional[A]) extends BijectiveLens[A, A] {
    def toView(m: A): A = frac.times(m, k)

    def toModel(v: A): A = frac.div(v, k)
  }

  /**
   * A simple lens with returns the identity
   */
  class NeutralLens[A] extends BijectiveLens[A, A] {
    def toView(m: A): A = m

    def toModel(v: A): A = v
  }
}
