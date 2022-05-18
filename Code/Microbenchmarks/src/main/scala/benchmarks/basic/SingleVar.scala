package benchmarks.basic

import benchmarks.EngineParam
import org.openjdk.jmh.annotations._
import rescala.Schedulers
import rescala.interface.RescalaInterface

import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.{ReadWriteLock, ReentrantReadWriteLock}

@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 3, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Fork(3)
@Threads(1)
@State(Scope.Benchmark)
class SingleVar {

  var engine: RescalaInterface              = _
  final lazy val engineT                          = engine
  implicit def scheduler: engineT.Scheduler = engineT.scheduler

  var source: engineT.Var[Boolean] = _
  var current: Boolean             = _
  var lock: ReadWriteLock          = _

  @Setup
  def setup(engineParam: EngineParam): Unit = {
    engine = engineParam.engine
    current = false
    source = engineT.Var(current)
    if (engineParam.engine == Schedulers.unmanaged) lock = new ReentrantReadWriteLock()
  }

  @Benchmark
  def write(): Unit = {
    if (lock == null) {
      current = !current
      source.set(current)
    } else {
      lock.writeLock().lock()
      try {
        current = !current
        source.set(current)
      } finally lock.writeLock().unlock()
    }
  }

  @Benchmark
  def read(): Boolean = {
    if (lock == null) {
      source.readValueOnce
    } else {
      lock.readLock().lock()
      try {
        source.readValueOnce
      } finally lock.readLock().unlock()
    }
  }

}
