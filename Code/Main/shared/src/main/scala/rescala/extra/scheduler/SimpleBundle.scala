package rescala.extra.scheduler

import rescala.core.Core
import rescala.operator.Observing

import scala.collection.mutable.{ArrayBuffer, ListBuffer}

trait SimpleBundle extends Core with Observing {
  type State[V] <: SimpleState[V]

  class SimpleState[V](var value: V) {

    var outgoing: Set[Derived]  = Set.empty
    var incoming: Set[ReSource] = Set.empty
    var discovered              = false
    var dirty                   = false
    var done                    = false
    def reset(v: V): Unit = {
      discovered = false
      dirty = false
      done = false
      value = v
    }

    override def toString: String =
      s"State(outgoing = $outgoing, discovered = $discovered, dirty = $dirty, done = $done)"
  }

  def makeDerivedStructStateBundle[V](ip: V): State[V]

  class SimpleInitializer(afterCommitObservers: ListBuffer[Observation]) extends Initializer {
    override protected[this] def makeDerivedStructState[V](ip: V): State[V] = makeDerivedStructStateBundle(ip)

    private var createdReactives: Seq[Derived] = Seq.empty

    def drainCreated(): Seq[Derived] = {
      val tmp = createdReactives
      createdReactives = Seq.empty
      tmp
    }

    override protected[this] def initialize(
        reactive: Derived,
        incoming: Set[ReSource],
        needsReevaluation: Boolean
    ): Unit = {
      incoming.foreach { dep =>
        dep.state.outgoing += reactive
      }
      reactive.state.incoming = incoming
      reactive.state.discovered = needsReevaluation
      reactive.state.dirty = needsReevaluation
      createdReactives :+= reactive

      val predecessorsDone = incoming.forall(r => !r.state.discovered || r.state.done)
      // requires reev, any predecessor is dirty, but all discovered predecessors are already done
      val requiresReev = incoming.exists(_.state.dirty) && predecessorsDone
      // if discovered, we are mid reevaluation
      val discovered = incoming.exists(_.state.discovered)
      if (discovered && !predecessorsDone) {
        // do nothing, this reactive is reached by normal propagation later
      } else if (needsReevaluation || requiresReev) {
        Util.evaluate(reactive, SimpleTransaction(this), afterCommitObservers)
      } else if (predecessorsDone) reactive.state.done = true
    }

  }

  case class SimpleTransaction(override val initializer: SimpleInitializer) extends Transaction {
    override object accessTicket extends AccessTicket {
      override private[rescala] def access(reactive: ReSource): reactive.Value = reactive.state.value
    }
  }

  object SimpleScheduler extends SimpleSchedulerInterface

  trait SimpleSchedulerInterface extends DynamicInitializerLookup[SimpleTransaction] with Scheduler {

    override def schedulerName: String = "Simple"

    var idle = true

    def reset(r: ReSource) = r.state.reset(r.commit(r.state.value))

    def beforeCleanupHook(all: Seq[ReSource], initialWrites: Set[ReSource]): Unit = ()

    override def forceNewTransaction[R](initialWrites: Set[ReSource], admissionPhase: AdmissionTicket => R): R =
      synchronized {
        if (!idle) throw new IllegalStateException("Scheduler is not reentrant")
        idle = false
        val afterCommitObservers: ListBuffer[Observation] = ListBuffer.empty
        val res =
          try {
            val creation    = new SimpleInitializer(afterCommitObservers)
            val transaction = SimpleTransaction(creation)
            withDynamicInitializer(transaction) {
              // admission
              val admissionTicket: AdmissionTicket = new AdmissionTicket(transaction, initialWrites) {
                override private[rescala] def access(reactive: ReSource): reactive.Value = reactive.state.value
              }
              val admissionResult = admissionPhase(admissionTicket)
              val sources = admissionTicket.initialChanges.values.collect {
                case iv if iv.writeValue(iv.source.state.value, iv.source.state.value = _) => iv.source
              }.toSeq

              creation.drainCreated().foreach(reset)

              val initial = sources.flatMap { s =>
                s.state.dirty = true
                s.state.done = true
                s.state.discovered = true
                s.state.outgoing
              }

              initial.foreach { r =>
                r.state.dirty = true
              }

              // propagation
              val sorted = Util.toposort(initial)
              Util.evaluateAll(sorted, transaction, afterCommitObservers).foreach(reset)
              // evaluate everything that was created, but not accessed, and requires ignition
              val created = creation.drainCreated()
              Util.evaluateAll(created, transaction, afterCommitObservers).foreach(reset)
              assert(creation.drainCreated().isEmpty)

              beforeCleanupHook(created ++ sorted ++ initialWrites, initialWrites)

              // cleanup
              initial.foreach(reset)
              created.foreach(reset)
              sources.foreach(reset)
              sorted.foreach(reset)

              // wrapup
              if (admissionTicket.wrapUp != null) admissionTicket.wrapUp(transaction.accessTicket)
              admissionResult
            }
          } finally {
            idle = true
          }
        afterCommitObservers.foreach(_.execute())
        res
      }

    override private[rescala] def singleReadValueOnce[A](reactive: Interp[A]): A = {
      val id = reactive
      id.interpret(id.state.value)
    }
  }

  object Util {
    def toposort(rem: Seq[Derived]): Seq[Derived] = {
      val sorted = ArrayBuffer[Derived]()

      def _toposort(rem: Derived): Unit = {
        if (rem.state.discovered) ()
        else {
          rem.state.discovered = true
          rem.state.outgoing.foreach(_toposort)
          sorted += rem
        }
      }

      rem.foreach(_toposort)
      // need toSeq for 2.13, where Seq is immutable
      sorted.toSeq
    }

    @scala.annotation.tailrec
    def evaluateAll(
        evaluatees: Seq[Derived],
        creation: SimpleTransaction,
        afterCommitObservers: ListBuffer[Observation]
    ): Seq[Derived] = {
      // first one where evaluation detects glitch
      val glitched = evaluatees.reverseIterator.find { r =>
        if (r.state.done) false
        else if (r.state.dirty) {
          Util.evaluate(r, creation, afterCommitObservers)
        } else false
      }
      glitched match {
        case None => evaluatees
        case Some(reactive) =>
          val evaluateNext = evaluatees.filterNot(_.state.done) ++ creation.initializer.drainCreated()
          evaluateNext.foreach(_.state.discovered = false)
          evaluateAll(Util.toposort(evaluateNext), creation, afterCommitObservers)
      }
    }

    def evaluate(
        reactive: Derived,
        creationTicket: SimpleTransaction,
        afterCommitObservers: ListBuffer[Observation]
    ): Boolean = {
      var potentialGlitch = false
      val dt = new ReevTicket[reactive.Value](creationTicket, reactive.state.value) {
        override def dynamicAccess(input: ReSource): input.Value = {
          if (input.state.discovered && !input.state.done) {
            potentialGlitch = true
          }
          input.state.value
        }
        override def staticAccess(input: ReSource): input.Value = input.state.value
      }
      val reev = reactive.reevaluate(dt)
      reev.inputs().foreach { newDeps =>
        val incoming = reactive.state.incoming
        reactive.state.incoming = newDeps
        val added   = newDeps diff incoming
        val removed = incoming diff newDeps
        added.foreach { input =>
          input.state.outgoing = input.state.outgoing + reactive
        }
        removed.foreach { input =>
          input.state.outgoing = input.state.outgoing - reactive
        }
      }

      if (potentialGlitch) true
      else {
        if (reev.activate) reactive.state.outgoing.foreach(_.state.dirty = true)
        reev.forValue(reactive.state.value = _)
        reev.forEffect(o => afterCommitObservers.append(o))
        reactive.state.done = true
        false
      }

    }
  }
}
