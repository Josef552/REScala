package benchmarks.simple

import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.{Lock, ReentrantLock}

import benchmarks.{EngineParam, Size, Step, Workload}
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.{BenchmarkParams, ThreadParams}
import rescala.propagation.Turn
import rescala.engines.Engine
import rescala.reactives.{Signal, SignalImpl, Signals}

@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
@Threads(4)
@State(Scope.Benchmark)
class MultiReverseFan[S <: rescala.graph.Struct] {

  implicit var engine: Engine[S, Turn[S]] = _

  var sources: Array[rescala.reactives.Var[Int, S]] = _
  var results: Array[SignalImpl[Int, S]] = _
  var locks: Array[Lock] = null
  var groupSize: Int = _

  @Setup
  def setup(params: BenchmarkParams, size: Size, step: Step, engineParam: EngineParam[S], work: Workload) = {
    engine = engineParam.engine
    val localEngine = engine;
    import localEngine._
    val threads = params.getThreads

    sources = Array.fill(threads)(Var(step.get()))
    groupSize = if (threads > size.size) threads / size.size else 1

    val intermediate = sources.map(_.map { v => {work.consume(); v + 1} }).grouped(groupSize)
    results = intermediate.map { sigs =>
      Signals.static(sigs.toSeq: _*) { t => val r = sigs.foldLeft(0)((a, v) => v.get(t) + a); work.consumeSecondary(); r }
    }.toArray

    if (engineParam.engineName == "unmanaged") locks = Array.fill(threads / groupSize)(new ReentrantLock())

  }

  @Benchmark
  def run(step: Step, params: ThreadParams): Unit = {
    val index = params.getThreadIndex
    if (locks == null) sources(index).set(step.run())
    else {
      locks(index / groupSize).lock()
      try {
        sources(index).set(step.run())
      }
      finally locks(index / groupSize).unlock()
    }
  }
}
