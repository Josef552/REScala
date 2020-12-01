package tests.rescala.concurrency.philosophers

import rescala.core.{ReName, Struct}
import rescala.interface.RescalaInterface
import tests.rescala.concurrency.philosophers.PhilosopherTable._

class DynamicPhilosopherTable[S <: Struct](philosopherCount: Int, work: Long)(ri: RescalaInterface[S])
    extends PhilosopherTable(philosopherCount, work)(ri) {
  import interface.{Var, Signal, implicitScheduler}

  override def createTable(tableSize: Int): Seq[Seating[S]] = {
    def mod(n: Int): Int = (n + tableSize) % tableSize

    val phils = for (i <- 0 until tableSize) yield Var[Philosopher](Thinking)(s"Phil($i)")

    val forks = for (i <- 0 until tableSize) yield {
      val nextCircularIndex     = mod(i + 1)
      implicit val name: ReName = s"Fork($i, $nextCircularIndex)"
      val left                  = phils(i)
      val right                 = phils(nextCircularIndex)
      Signal {
        left() match {
          case Hungry => Taken(i.toString)
          case Thinking =>
            right() match {
              case Hungry   => Taken(nextCircularIndex.toString)
              case Thinking => Free
            }
        }
      }
    }

    for (i <- 0 until tableSize) yield {
      val ownName               = i.toString
      val fork1                 = forks(i)
      val fork2                 = forks(mod(i - 1))
      implicit val name: ReName = s"Vision($i)"
      val vision = Signal {
        fork1() match {
          case Taken(name) if name != ownName => WaitingFor(name)
          case Taken(`ownName`)               => Eating
          case Free => fork2() match {
              case Free        => Ready
              case Taken(name) => WaitingFor(name)
            }
        }
      }
      Seating(i, phils(i), fork1, fork2, vision)
    }
  }

}
