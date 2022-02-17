package benchmarks.lattices

import kofre.causality.impl.ArrayRanges
import org.openjdk.jmh.annotations._

import java.util.concurrent.TimeUnit

@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 3, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Fork(3)
@Threads(1)
@State(Scope.Thread)
class ArrayContextBench {

  @Param(Array("1", "1000"))
  var size: Int = _

  var rep1Set: ArrayRanges        = _
  var rep1SetPlusOne: ArrayRanges = _
  var rep2Set: ArrayRanges        = _
  var rep1single: ArrayRanges     = _

  private def makeRep(mul: Int, off: Int, len: Int): ArrayRanges = {
    val ranges = Range(0, size).map(i => Range(i * mul + off, i * mul + len + off))
    new ArrayRanges(ranges.flatMap(r => Array(r.start, r.end)).toArray)
  }

  @Setup
  def setup(): Unit = {
    rep1Set = makeRep(10, 0, 7)
    rep2Set = makeRep(10, 5, 7)
    rep1SetPlusOne = rep1Set.add(5)
  }

  @Benchmark
  def merge() = rep1Set.merge(rep2Set)

  @Benchmark
  def mergeSelf() = (rep1Set merge rep1Set)

  @Benchmark
  def mergeSelfPlusOne() = (rep1Set merge rep1SetPlusOne)

}
