package rescala.pipelining.tests

import rescala.graph.Reactive
import rescala.pipelining.{Pipeline, PipelineEngine, PipelineStruct, PipeliningTurn}
import rescala.propagation.Turn
import rescala.reactives.SignalImpl

import scala.collection.immutable.Queue
import scala.util.Random

object PipelineTestUtils {

  private val rand = new Random

  type S = PipelineStruct.type

  def frameTurns(f : Reactive[S]) : Queue[Turn[S]] = {
    Pipeline.pipelineFor(f).getPipelineFrames().map { _.turn}
  }

  def randomWait[A](op: => A) : A = {
    val waitBefore = rand.nextInt(10)
    val waitAfter = rand.nextInt(10)
    Thread.sleep(waitBefore.toLong)
    val result = op
    Thread.sleep(waitAfter.toLong)
    result
  }

  def createThread(job : => Any) : Thread = {
    new Thread(new Runnable() {
      override def run() = {
        job
      }
    }
    )
  }

  def readLatestValue(reader : PipeliningTurn => Unit)(implicit engine : PipelineEngine) = {
    val dummyTurn = engine.makeTurn
    engine.addTurn(dummyTurn)
    reader(dummyTurn)
    engine.turnCompleted(dummyTurn)
  }

}

class ValueTracker[T](s : SignalImpl[T, PipelineStruct.type])(implicit val engine: PipelineEngine) {
    var values : List[T] = List()
    private object valueLock

    s.observe(newValue => valueLock.synchronized{values :+= newValue})
    reset()

    def reset() = valueLock.synchronized{values = List()}
  }
