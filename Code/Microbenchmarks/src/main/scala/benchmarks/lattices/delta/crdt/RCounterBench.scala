package benchmarks.lattices.delta.crdt

import org.openjdk.jmh.annotations._
import rescala.extra.lattices.delta.crdt.reactive.ReactiveDeltaCRDT
import kofre.decompose.interfaces.RCounterInterface.RCounter
import kofre.decompose.interfaces.RCounterInterface.RCounterSyntax

import java.util.concurrent.TimeUnit

@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 3, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Fork(3)
@Threads(1)
@State(Scope.Thread)
class RCounterBench {

  @Param(Array("1", "10", "100", "1000"))
  var numReplicas: Int = _

  var counter: ReactiveDeltaCRDT[RCounter] = _

  @Setup
  def setup(): Unit = {
    counter = (1 until numReplicas).foldLeft(ReactiveDeltaCRDT[RCounter]("0").increment()) {
      case (c, n) =>
        val delta = ReactiveDeltaCRDT[RCounter](n.toString).increment().deltaBuffer.head
        c.applyDelta(delta)
    }
  }

  @Benchmark
  def value(): Int = counter.value

  @Benchmark
  def fresh(): ReactiveDeltaCRDT[RCounter] = counter.fresh()

  @Benchmark
  def increment(): ReactiveDeltaCRDT[RCounter] = counter.increment()

  @Benchmark
  def decrement(): ReactiveDeltaCRDT[RCounter] = counter.decrement()

  @Benchmark
  def reset(): ReactiveDeltaCRDT[RCounter] = counter.reset()
}
