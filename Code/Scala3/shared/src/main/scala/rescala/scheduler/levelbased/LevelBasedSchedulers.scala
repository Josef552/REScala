package rescala.scheduler.levelbased

import rescala.scheduler.{Levelbased, levelbased}
import rescala.interface.RescalaInterface
import rescala.operator._
import rescala.core.Core

object LevelBasedSchedulers extends RescalaInterface with Levelbased with EventApi with SignalApi with Sources with DefaultImplementations with Observing with Core {

  type State[V] = LevelState[V]

  private[rescala] class SimpleNoLock extends LevelBasedTransaction {
    override protected def makeDerivedStructState[V](ip: V): State[V] = {
      new LevelState(ip)
    }
    override def releasePhase(): Unit = ()
    override def preparationPhase(initialWrites: Set[ReSource]): Unit = {}
    override def beforeDynamicDependencyInteraction(dependency: ReSource): Unit = {}
  }

  override def scheduler: Scheduler = new TwoVersionScheduler[SimpleNoLock] {
    override protected def makeTransaction(priorTx: Option[SimpleNoLock]): SimpleNoLock = new SimpleNoLock
    override def schedulerName: String                                                  = "Synchron"
    override def forceNewTransaction[R](
        initialWrites: Set[ReSource],
        admissionPhase: AdmissionTicket => R
    ): R =
      synchronized { super.forceNewTransaction(initialWrites, admissionPhase) }
  }
}