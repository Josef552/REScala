package test.kofre

import kofre.primitives.{LastWriterWins, MultiValueRegister, VectorClock}
import kofre.sets.ORSet
import kofre.{IdUtil, Lattice}
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import test.kofre.DataGenerator.{*, given}

import javax.swing.plaf.multi.MultiListUI

object DataGenerator {

  given arbId: Arbitrary[IdUtil.Id] = Arbitrary(Gen.oneOf('a' to 'g').map(_.toString))

  given arbVersion: Arbitrary[VectorClock] = Arbitrary(for {
    ids: Set[IdUtil.Id] <- Gen.nonEmptyListOf(arbId.arbitrary).map(_.toSet)
    value: List[Long]   <- Gen.listOfN(ids.size, Gen.oneOf(0L to 100L))
  } yield VectorClock.fromMap(ids.zip(value).toMap))

  given arbLww: Arbitrary[LastWriterWins[Int]] = Arbitrary(
    for {
      time  <- Gen.long
      value <- Gen.choose(Int.MinValue, Int.MaxValue)
    } yield LastWriterWins(time, value)
  )

  given Lattice[Int] = _ max _

  given arbORSet[A: Arbitrary]: Arbitrary[ORSet[A]] = Arbitrary(for {
    added   <- Gen.nonEmptyListOf(Arbitrary.arbitrary[A])
    removed <- Gen.listOf(Gen.oneOf(added))
  } yield {
    val a = added.foldLeft(ORSet.empty[A])((s, v) => Lattice.merge(s, s.add(v)))
    removed.foldLeft(a)((s, v) => Lattice.merge(s, s.remove(v)))
  })

  given arbMVR[A: Arbitrary]: Arbitrary[MultiValueRegister[A]] =
    val pairgen = for {
      version <- arbVersion.arbitrary
      value   <- Arbitrary.arbitrary[A]
    } yield (version, value)
    val map = Gen.listOf(pairgen).map(vs => MultiValueRegister(vs.toMap))
    Arbitrary(map)
}

class VectorClockLattice extends LatticeMergeTest[VectorClock]
class LWWLatice          extends LatticeMergeTest[LastWriterWins[Int]]
class OrSetLatice        extends LatticeMergeTest[ORSet[Int]]
class MVRLattice         extends LatticeMergeTest[MultiValueRegister[Int]]

abstract class LatticeMergeTest[A: Arbitrary: Lattice] extends AnyFreeSpec with ScalaCheckDrivenPropertyChecks {

  "idempotent" in forAll { (a: A, b: A) =>
    val ab  = Lattice.merge(a, b)
    val abb = Lattice.merge(ab, b)
    assert(ab === abb)
  }

  "commutative" in forAll { (a: A, b: A) =>
    assert(Lattice.merge(b, a) === Lattice.merge(a, b))
  }

  "associative" in forAll { (a: A, b: A, c: A) =>
    val ab   = Lattice.merge(a, b)
    val bc   = Lattice.merge(b, c)
    val abc  = Lattice.merge(ab, c)
    val abc2 = Lattice.merge(a, bc)
    assert(abc === abc2)
  }

}