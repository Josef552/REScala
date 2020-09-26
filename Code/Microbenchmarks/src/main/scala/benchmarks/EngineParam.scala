package benchmarks

import org.openjdk.jmh.annotations.{Param, Scope, State}
import rescala.Schedulers
import rescala.core.{Scheduler, Struct}
import rescala.interface.RescalaInterface

@State(Scope.Benchmark)
class EngineParam[S <: Struct] {
  @Param(Array("synchron", "parrp", "fullmv", "simple"))
  var engineName: String = _

  def engine: RescalaInterface[S] =
    RescalaInterface.interfaceFor(engineName match {
      case "fullmv" =>
        new rescala.fullmv.FullMVEngine(scala.concurrent.duration.Duration.Zero, "benchmark").asInstanceOf[Scheduler[S]]
      case other => Schedulers.byName[S](other)
    })
}
