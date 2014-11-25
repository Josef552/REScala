package rescala.propagation.turns.instances

import rescala.propagation.Reactive
import rescala.propagation.turns.Turn
import rescala.propagation.turns.instances.Evaluator.Result

abstract class AbstractTurn extends Turn {
  outer =>
  implicit def currentTurn: AbstractTurn = this

  protected var toCommit = Set[Reactive]()
  protected var afterCommitHandlers = List[() => Unit]()

  protected var initialSources: List[Reactive] = Nil

  def handleDiff(res: Result): Result = {
    res.getDiff.foreach { diff =>
      diff.novel foreach acquireDynamic
      diff.removed foreach unregister(res.head)
      diff.added foreach register(res.head)
    }
    res
  }

  val levelQueue = new LevelQueue(evaluate)

  def evaluate(r: Reactive): Unit = handleDiff(Evaluator.evaluate(r)).requeue(levelQueue.enqueue)

  def acquireDynamic(reactive: Reactive): Unit

  def lockingPhase(): Unit
  def realeasePhase(): Unit

  def register(dependant: Reactive)(dependency: Reactive): Unit = {
    dependency.dependants.transform(_ + dependant)
  }

  def unregister(dependant: Reactive)(dependency: Reactive): Unit = {
    acquireDynamic(dependency)
    dependency.dependants.transform(_ - dependant)
  }


  def propagationPhase(): Unit = {
    initialSources.foreach(levelQueue.enqueue(0))
    levelQueue.evaluateQueue()
  }

  def markForCommit(reactive: Reactive): Unit = {
    toCommit += reactive
  }

  def commitPhase() = toCommit.foreach(_.commit(this))

  override def afterCommit(handler: => Unit) = afterCommitHandlers ::= handler _

  def observerPhase() = afterCommitHandlers.foreach(_())

  def create[T <: Reactive](dependencies: Set[Reactive])(f: => T): T = {
    val reactive = f
    dependencies.foreach(register(reactive))
    ensureLevel(reactive, dependencies)
    reactive
  }

  def createDynamic[T <: Reactive](dependencies: Set[Reactive])(f: => T): T = {
    val reactive = f
    ensureLevel(reactive, dependencies)
    evaluate(reactive)
    reactive
  }

  def ensureLevel(dependant: Reactive, dependencies: Set[Reactive]): Int =
    if (dependencies.isEmpty) 0
    else {
      val newLevel = dependencies.map(_.level.get).max + 1
      dependant.level.transform(math.max(newLevel, _))
    }

  /** admits a new source change */
  override def admit(source: Reactive)(setPulse: => Boolean): Unit = if (setPulse) initialSources ::= source

}