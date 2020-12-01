package benchmarks.simple

import java.util.concurrent.TimeUnit

import benchmarks.{EngineParam, Size, Step, Workload}
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.{BenchmarkParams, ThreadParams}
import rescala.Schedulers
import rescala.core.Struct
import rescala.interface.RescalaInterface
import rescala.operator.Signal

@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 3, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Fork(3)
@Threads(4)
@State(Scope.Benchmark)
class ReverseFan[S <: Struct] {

  var engine: RescalaInterface[S] = _

  var sources: Array[rescala.operator.Var[Int, S]] = _
  var result: Signal[Int, S]                       = _
  var isManual: Boolean                             = false

  @Setup
  def setup(params: BenchmarkParams, size: Size, step: Step, engineParam: EngineParam[S], work: Workload) = {
    engine = engineParam.engine
    val localEngine = engine
    import localEngine._
    sources = Array.fill(16)(Var(step.get()))
    val intermediate = sources.map(_.map { v => { work.consume(); v + 1 } })
    result = Signals.lift(intermediate.toSeq) { values => work.consumeSecondary(); values.sum }
    if (engine.scheduler == Schedulers.unmanaged) isManual = true

  }

  @Benchmark
  def run(step: Step, params: ThreadParams): Unit =
    if (isManual) synchronized { sources(params.getThreadIndex).set(step.run())(engine.scheduler) }
    else sources(params.getThreadIndex).set(step.run())(engine.scheduler)
}
