package benchmarks.lattices.delta

import kofre.decompose.Dot
import org.openjdk.jmh.annotations._
import rescala.extra.lattices.delta.DietCC.DietMapCContext

import java.util.concurrent.TimeUnit

@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 3, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Fork(3)
@Threads(1)
@State(Scope.Thread)
class DietMapCContextBench {

  @Param(Array("1", "1000"))
  var size: Int = _

  var cca: DietMapCContext       = _
  var ccb: DietMapCContext       = _
  var cca1: DietMapCContext      = _
  var ccaSingle: DietMapCContext = _


  private def makeCContext(replicaID: String, mul: Int, off: Int, len: Int): DietMapCContext = {
    val ranges = Range(0, size).map(i => Range(i * mul + off, i * mul + len + off))
    val dots = ranges.flatten.map(Dot(replicaID, _)).toSet
    DietMapCContext.fromSet(dots)
  }

  @Setup
  def setup(): Unit = {
    cca = makeCContext("a", 10, 0, 7)
    ccb = makeCContext("b", 10, 5, 7)
    cca1 = DietMapCContext.union(cca, DietMapCContext.fromSet(Set(Dot("b", 5))))
    ccaSingle = DietMapCContext.fromSet(Set(Dot("a", size + 10)))
  }

  @Benchmark
  def merge = DietMapCContext.union(cca, ccb)

  @Benchmark
  def mergeSelf = DietMapCContext.union(cca, cca)

  @Benchmark
  def mergeSelfPlusOne = DietMapCContext.union(cca, cca1)
}
