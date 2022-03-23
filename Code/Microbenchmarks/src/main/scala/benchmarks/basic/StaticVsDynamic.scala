package benchmarks.basic

import benchmarks.{EngineParam, Step}
import org.openjdk.jmh.annotations._
import rescala.interface.RescalaInterface

import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReadWriteLock

@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 3, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Fork(3)
@Threads(1)
@State(Scope.Thread)
class StaticVsDynamic {

  var engine: RescalaInterface = _
  lazy val stableEngine        = engine
  import stableEngine._

  @Param(Array("true", "false"))
  var static: Boolean = _

  var source: Var[Boolean] = _
  var current: Boolean     = _
  var lock: ReadWriteLock  = _
  var a: Var[Int]          = _
  var b: Var[Int]          = _
  var res: Signal[Int]     = _

  @Setup
  def setup(engineParam: EngineParam): Unit = {
    engine = engineParam.engine
    current = true
    source = stableEngine.Var(current)
    a = stableEngine.Var { 10 }
    b = stableEngine.Var { 20 }

    if (static) Signals.static(source, a, b) { st =>
      if (st.dependStatic(source)) st.dependStatic(a) else st.dependStatic(b)
    }
    else Signal.dynamic { if (source()) a() else b() }

  }

  @Benchmark
  def switchOnly(): Unit = {
    current = !current
    source.set(current)
  }

  @Benchmark
  def aOnly(step: Step): Unit = {
    a.set(step.run())
  }
  @Benchmark
  def bOnly(step: Step): Unit = {
    b.set(step.run())
  }
}
