package rescala.benchmarks.measureSizes

import kofre.causality.impl.ArrayRanges
import org.openjdk.jol.info.GraphLayout
import rescala.{Schedulers, default}

object ObjectSizes {

  def measure(name: String, roots: Any*): Unit = {
    println(s"======= $name")
    println(GraphLayout.parseInstance(roots: _*).toFootprint)
  }

  def main(args: Array[String]): Unit = {
    measure("empty range", ArrayRanges.empty)
    measure("6 elem range", ArrayRanges.empty.add(1).add(2).add(3).add(4).add(5).add(6))
    measure("var empty", rescala.default.Var.empty)
    measure("var 5", rescala.default.Var(5))
    measure("default empty signal", rescala.default.Signal {})
    measure("default empty signal x 10", List.fill(100)(rescala.default.Signal {}))
    measure("synchron empty signal", rescala.Schedulers.synchron.Signal {})

    def ptx = new default.ParRPTransaction(new rescala.parrp.Backoff(), None)
    measure("transaction", List.fill(100)(ptx))
    measure(
      "reev ticket",
      new rescala.default.ReevTicket(ptx, (), new default.AccessHandler {
        override def staticAccess(reactive: default.ReSource): reactive.Value  = ???
        override def dynamicAccess(reactive: default.ReSource): reactive.Value = ???
      })
    )

    def stx = new Schedulers.synchron.SimpleNoLock()
    measure("nolock transaction", List.fill(100)(stx))
    measure(
      "nolock reev ticket",
      new rescala.Schedulers.synchron.ReevTicket(stx, (), new rescala.Schedulers.synchron.AccessHandler {
        override def staticAccess(reactive: rescala.Schedulers.synchron.ReSource): reactive.Value  = ???
        override def dynamicAccess(reactive: rescala.Schedulers.synchron.ReSource): reactive.Value = ???
      })
      )
  }

}
