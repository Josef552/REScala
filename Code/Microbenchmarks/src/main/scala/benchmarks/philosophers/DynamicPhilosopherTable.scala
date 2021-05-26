package benchmarks.philosophers

import benchmarks.philosophers.PhilosopherTable._

import rescala.interface.RescalaInterface

class DynamicPhilosopherTable(philosopherCount: Int, work: Long)(override val engine: RescalaInterface)
    extends PhilosopherTable(philosopherCount, work)(engine) {

  import engine._

  override def createTable(tableSize: Int): Seq[Seating] = {
    def mod(n: Int): Int = (n + tableSize) % tableSize

    val phils = for (i <- 0 until tableSize) yield Var[Philosopher](Thinking)(engine.implicitScheduler)

    val forks = for (i <- 0 until tableSize) yield {
      val nextCircularIndex = mod(i + 1)
      Signal.dynamic {
        phils(i)() match {
          case Eating => Taken(i.toString)
          case Thinking =>
            phils(nextCircularIndex)() match {
              case Eating   => Taken(nextCircularIndex.toString)
              case Thinking => Free
            }
        }
      }(engine.implicitScheduler)

    }

    for (i <- 0 until tableSize) yield {
      val ownName = i.toString
      val vision = Signal.dynamic {
        forks(i)() match {
          case Taken(name) if name != ownName => BlockedBy(name)
          case Taken(`ownName`)               => Done
          case Free => forks(mod(i - 1))() match {
              case Free        => Ready
              case Taken(name) => BlockedBy(name)
            }
        }
      }
      Seating(i, phils(i), forks(i), forks(mod(i - 1)), vision)
    }
  }

}

class HalfDynamicPhilosopherTable(philosopherCount: Int, work: Long)(
    override val engine: RescalaInterface
) extends PhilosopherTable(philosopherCount, work)(engine) {

  import engine._

  override def createTable(tableSize: Int): Seq[Seating] = {
    def mod(n: Int): Int = (n + tableSize) % tableSize

    val phils = for (i <- 0 until tableSize) yield Var[Philosopher](Thinking)

    val forks = for (i <- 0 until tableSize) yield {
      val nextCircularIndex = mod(i + 1)
      Signals.lift(phils(i), phils(nextCircularIndex))(calcFork(i.toString, nextCircularIndex.toString))
    }

    for (i <- 0 until tableSize) yield {
      val ownName = i.toString
      val vision = Signal.dynamic {
        forks(i)() match {
          case Taken(name) if name != ownName => BlockedBy(name)
          case Taken(`ownName`)               => Done
          case Free => forks(mod(i - 1))() match {
              case Free        => Ready
              case Taken(name) => BlockedBy(name)
            }
        }
      }
      Seating(i, phils(i), forks(i), forks(mod(i - 1)), vision)
    }
  }

}

class OtherHalfDynamicPhilosopherTable(philosopherCount: Int, work: Long)(
    override implicit val engine: RescalaInterface
) extends PhilosopherTable(philosopherCount, work)(engine) {

  import engine.{Signal, Signals, Var, implicitScheduler}

  override def createTable(tableSize: Int): Seq[Seating] = {
    def mod(n: Int): Int = (n + tableSize) % tableSize

    val phils = for (i <- 0 until tableSize) yield Var[Philosopher](Thinking)

    val forks = for (i <- 0 until tableSize) yield {
      val nextCircularIndex = mod(i + 1)
      Signal.dynamic {
        phils(i)() match {
          case Eating => Taken(i.toString)
          case Thinking =>
            phils(nextCircularIndex)() match {
              case Eating   => Taken(nextCircularIndex.toString)
              case Thinking => Free
            }
        }
      }

    }

    for (i <- 0 until tableSize) yield {
      val vision = Signals.lift(forks(i), forks(mod(i - 1)))(calcVision(i.toString))
      Seating(i, phils(i), forks(i), forks(mod(i - 1)), vision)
    }

  }

}
