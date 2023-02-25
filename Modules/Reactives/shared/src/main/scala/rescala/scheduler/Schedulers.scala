package rescala.scheduler

import rescala.core.{AdmissionTicket, ReSource, Scheduler}
import rescala.operator.Interface
import rescala.scheduler.{Levelbased, Sidup, TopoBundle}

object Schedulers extends PlatformSchedulers {

  object NoLock extends Levelbased {
    type State[V] = LevelState[V]
    private[rescala] class SimpleNoLock extends LevelBasedTransaction {
      override protected def makeDerivedStructState[V](initialValue: V): State[V] = new LevelState(initialValue)
      override def releasePhase(): Unit                                           = ()
      override def preparationPhase(initialWrites: Set[ReSource.of[State]]): Unit = {}
      override def beforeDynamicDependencyInteraction(dependency: ReSource): Unit = {}
    }

    val unmanaged: Scheduler[State] =
      new TwoVersionScheduler[SimpleNoLock] {
        override protected def makeTransaction(priorTx: Option[SimpleNoLock]): SimpleNoLock = new SimpleNoLock()

        override def schedulerName: String = "Unmanaged"
      }

    val synchron: Scheduler[State] = new TwoVersionScheduler[SimpleNoLock] {
      override protected def makeTransaction(priorTx: Option[SimpleNoLock]): SimpleNoLock = new SimpleNoLock

      override def schedulerName: String = "Synchron"

      override def forceNewTransaction[R](
          initialWrites: Set[ReSource.of[State]],
          admissionPhase: AdmissionTicket[State] => R
      ): R =
        synchronized {
          super.forceNewTransaction(initialWrites, admissionPhase)
        }
    }
  }

  val unmanaged: Interface = Interface.from(NoLock.unmanaged)

  val synchron: Interface = Interface.from(NoLock.synchron)

  object toposort extends Interface with TopoBundle {
    override def makeDerivedStructStateBundle[V](ip: V): TopoState[V] = new TopoState[V](ip)
    override type State[V]       = TopoState[V]
    override type BundleState[V] = TopoState[V]
    override val scheduler: Scheduler[State] = TopoScheduler
  }

  object sidup extends Interface {
    val bundle: Sidup = new Sidup {}
    override type BundleState[V] = bundle.State[V]
    val scheduler: Scheduler[BundleState] = new bundle.TwoVersionScheduler[bundle.SidupTransaction] {
      override protected def makeTransaction(priorTx: Option[bundle.SidupTransaction]): bundle.SidupTransaction =
        new bundle.SidupTransaction
      override def schedulerName: String = "SidupSimple"
      override def forceNewTransaction[R](
          initialWrites: Set[ReSource.of[BundleState]],
          admissionPhase: AdmissionTicket[BundleState] => R
      ): R =
        synchronized { super.forceNewTransaction(initialWrites, admissionPhase) }
    }
  }

  override def byName(name: String): Interface =
    name match {
      case "synchron"  => synchron
      case "unmanaged" => unmanaged
      case "toposort"  => toposort
      case "sidup"     => sidup
      case other       => super.byName(name)
    }

}
