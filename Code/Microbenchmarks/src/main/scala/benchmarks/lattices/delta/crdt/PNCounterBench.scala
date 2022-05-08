package benchmarks.lattices.delta.crdt

import org.openjdk.jmh.annotations._
import kofre.predef.PosNegCounter
import kofre.decompose.containers.DeltaBufferRDT

import java.util.concurrent.TimeUnit

@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 3, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Fork(3)
@Threads(1)
@State(Scope.Thread)
class PNCounterBench {

  @Param(Array("1", "10", "100", "1000"))
  var numReplicas: Int = _

  var counter: DeltaBufferRDT[PosNegCounter] = _

  @Setup
  def setup(): Unit = {
    counter = (1 until numReplicas).foldLeft(DeltaBufferRDT[PosNegCounter]("0").inc()) {
      case (c, n) =>
        val delta = DeltaBufferRDT[PosNegCounter](n.toString).inc().deltaBuffer.head
        c.applyDelta(delta)
    }
  }

  @Benchmark
  def value(): Int = counter.value

  @Benchmark
  def inc(): DeltaBufferRDT[PosNegCounter] = counter.inc()

  @Benchmark
  def dec(): DeltaBufferRDT[PosNegCounter] = counter.dec()
}
