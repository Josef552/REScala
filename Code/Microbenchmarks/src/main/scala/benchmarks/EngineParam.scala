package benchmarks

import org.openjdk.jmh.annotations.{Param, Scope, State}
import rescala.Schedulers
import rescala.interface.RescalaInterface

@State(Scope.Benchmark)
class EngineParam {
  @Param(Array("synchron", "parrp", "fullmv", "simple", "sidup"))
  var engineName: String = _

  def engine: RescalaInterface = Schedulers.byName(engineName)
}
