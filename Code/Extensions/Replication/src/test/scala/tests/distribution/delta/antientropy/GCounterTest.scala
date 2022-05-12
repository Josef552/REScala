package tests.distribution.delta.antientropy

import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import rescala.extra.lattices.delta.JsoniterCodecs._
import rescala.extra.replication.AntiEntropy
import kofre.decompose.containers.{AntiEntropyCRDT, Network}
import NetworkGenerators._
import kofre.predef.GrowOnlyCounter

import scala.collection.mutable

object GCounterGenerators {
  val genGCounter: Gen[AntiEntropyCRDT[GrowOnlyCounter]] = for {
    n <- Gen.posNum[Int]
  } yield {
    val network = new Network(0, 0, 0)
    val ae      = new AntiEntropy[GrowOnlyCounter]("a", network, mutable.Buffer())

    (0 until n).foldLeft(AntiEntropyCRDT[GrowOnlyCounter](ae)) {
      case (c, _) => c.inc()(AntiEntropyCRDT.allPermissions)
    }
  }

  implicit val arbGCounter: Arbitrary[AntiEntropyCRDT[GrowOnlyCounter]] = Arbitrary(genGCounter)
}

class GCounterTest extends AnyFreeSpec with ScalaCheckDrivenPropertyChecks {
  import GCounterGenerators._

  "inc" in forAll { counter: AntiEntropyCRDT[GrowOnlyCounter] =>
    val counterInc = counter.inc()

    assert(
      counterInc.value == counter.value + 1,
      s"Incrementing the counter should increase its value by 1, but ${counterInc.value} does not equal ${counter.value} + 1"
    )
  }

  "concurrent inc" in {
    val network = new Network(0, 0, 0)

    val aea = new AntiEntropy[GrowOnlyCounter]("a", network, mutable.Buffer("b"))
    val aeb = new AntiEntropy[GrowOnlyCounter]("b", network, mutable.Buffer("a"))

    val ca0 = AntiEntropyCRDT[GrowOnlyCounter](aea).inc()
    val cb0 = AntiEntropyCRDT[GrowOnlyCounter](aeb).inc()

    AntiEntropy.sync(aea, aeb)

    val ca1 = ca0.processReceivedDeltas()
    val cb1 = cb0.processReceivedDeltas()

    assert(
      ca1.value == 2,
      s"Concurrent increments should have the same behavior as sequential increments, but ${ca1.value} does not equal 2"
    )
    assert(
      cb1.value == 2,
      s"Concurrent increments should have the same behavior as sequential increments, but ${cb1.value} does not equal 2"
    )
  }

  "convergence" in forAll { (incA: Short, incB: Short, network: Network) =>
    val aea = new AntiEntropy[GrowOnlyCounter]("a", network, mutable.Buffer("b"))
    val aeb = new AntiEntropy[GrowOnlyCounter]("b", network, mutable.Buffer("a"))

    val ca0 = (0 until incA.toInt).foldLeft(AntiEntropyCRDT[GrowOnlyCounter](aea)) {
      case (c, _) => c.inc()
    }
    val cb0 = (0 until incB.toInt).foldLeft(AntiEntropyCRDT[GrowOnlyCounter](aeb)) {
      case (c, _) => c.inc()
    }

    AntiEntropy.sync(aea, aeb)
    network.startReliablePhase()
    AntiEntropy.sync(aea, aeb)

    val ca1 = ca0.processReceivedDeltas()
    val cb1 = cb0.processReceivedDeltas()

    assert(
      ca1.value == cb1.value,
      s"After synchronization messages were reliably exchanged all replicas should converge, but ${ca1.value} does not equal ${cb1.value}"
    )
  }
}
