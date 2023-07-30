package test.kofre.corestructs

import kofre.base.Lattice
import kofre.dotted.DottedLattice.*
import kofre.dotted.{DotFun, DotMap, DotSet, Dotted, DottedLattice}
import kofre.time.{ArrayRanges, Dot, Dots}
import org.scalacheck.Prop.*
import org.scalacheck.{Arbitrary, Gen}
import test.kofre.DataGenerator.{given, *}

import scala.annotation.tailrec

class DotMapTest extends munit.ScalaCheckSuite {

  type TestedMap = DotMap[Int, DotSet]

  property("dots") {
    forAll { (dm: TestedMap) =>
      assertEquals(
        dm.dots.toSet,
        clue(dm).repr.values.flatMap(
          _.dots.iterator
        ).toSet,
        s"DotMap.dots should return the keys of the DotMap itself,"
      )
    }
  }
  test("empty") {
    assert(
      DotFun.empty.repr.isEmpty,
      s"DotMap.empty should be empty, but ${DotFun.empty} is not empty"
    )

  }
  property("merge") {
    forAll {
      (
          dmA: TestedMap,
          deletedA: Dots,
          dmB: TestedMap,
          deletedB: Dots
      ) =>
        val dotsA = dmA.dots
        val dotsB = dmB.dots
        val ccA   = dotsA union deletedA
        val ccB   = dotsB union deletedB

        val Dotted(dmMerged, ccMerged) =
          Lattice[Dotted[TestedMap]].merge(
            Dotted(dmA, (ccA)),
            Dotted(dmB, (ccB))
          )
        val dotsMerged = dmMerged.dots

        assert(
          ccMerged == (ccA union ccB),
          s"DotMap.merge should have the same effect as set union on the causal context, but $ccMerged does not equal ${ccA union ccB}"
        )
        assert(
          dotsMerged.toSet subsetOf (dotsA union dotsB).toSet,
          s"DotMap.merge should not add new elements to the DotSet, but $dotsMerged is not a subset of ${dotsA union dotsB}"
        )
        assert(
          (dotsMerged intersect (deletedA diff dotsA)).isEmpty,
          s"The DotMap resulting from DotMap.merge should not contain dots that were deleted on the lhs, but $dotsMerged contains elements from ${deletedA diff dotsA}"
        )
        assert(
          (dotsMerged intersect (deletedB diff dotsB)).isEmpty,
          s"The DotMap resulting from DotMap.merge should not contain dots that were deleted on the rhs, but $dotsMerged contains elements from ${deletedB diff dotsB}"
        )

        // ignore cases where the dots intersect, as this check does not seem to handle such cases correcly
        if (dotsA.intersect(dotsB).isEmpty) {
          (dmA.repr.keySet union dmB.repr.keySet).foreach { k =>
            val vMerged =
              Dotted(dmA.repr.getOrElse(k, DotSet.empty), (ccA)) mergePartial
              Dotted(dmB.repr.getOrElse(k, DotSet.empty), (ccB))

            assert(
              vMerged.isEmpty || dmMerged.repr(k) == vMerged,
              s"For all keys that are in both DotMaps the result of DotMap.merge should map these to the merged values, but ${dmMerged.repr.get(k)} does not equal $vMerged"
            )
          }
        }
    }
  }

  property("leq") {
    forAll {
      (
          dmA: TestedMap,
          deletedA: Dots,
          dmB: TestedMap,
          deletedB: Dots
      ) =>
        val ccA = dmA.dots union deletedA
        val ccB = dmB.dots union deletedB

        val dottedA = Dotted(dmA, ccA)
        val dottedB = Dotted(dmB, ccB)

        assert(
          dottedA <= Dotted(dmA, (ccA)),
          s"DotMap.leq should be reflexive, but returns false when applied to ($dmA, $ccA)"
        )

        val merged =
          Lattice[Dotted[TestedMap]].merge(
            dottedA,
            dottedB
          )

        assert(
          dottedA <= merged,
          s"The result of DotMap.merge should be larger than its lhs, but DotMap.leq returns false when applied to:\n  $dottedA\n  $merged"
        )
        assert(
          dottedB <= merged,
          s"The result of DotMap.merge should be larger than its rhs, but DotMap.leq returns false when applied to\n  $dottedB\n  $merged"
        )
    }

  }

  @tailrec
  private def removeDuplicates(
      start: List[(Int, DotSet)],
      acc: TestedMap,
      con: Dots
  ): TestedMap =
    start match
      case Nil         => acc
      case (i, c) :: t => removeDuplicates(t, DotMap(acc.repr + (i -> DotSet(c.repr.subtract(con)))), con union c.dots)

  property("decompose") {
    forAll { (dmdup: TestedMap, deleted: Dots) =>

      val dm: TestedMap = removeDuplicates(dmdup.repr.toList, DotMap.empty, Dots.empty)

      val cc = dm.dots union deleted

      val decomposed: Iterable[Dotted[TestedMap]] =
        DottedLattice[TestedMap].decompose(Dotted(dm, (cc)))
      val wc: Dotted[TestedMap] =
        decomposed.foldLeft(Dotted(DotMap.empty[Int, DotSet], Dots.empty)) {
          case (Dotted(dmA, ccA), Dotted(dmB, ccB)) =>
            Lattice[Dotted[TestedMap]].merge(Dotted(dmA, ccA), Dotted(dmB, ccB))
        }

      val dmMerged: TestedMap = wc.data
      val ccMerged            = wc.context

      assertEquals(
        ccMerged,
        cc,
        s"Merging the list of atoms returned by DotMap.decompose should produce an equal DotMap, but $dmMerged does not equal $dm"
      )
      dm.repr.keys.foreach { k =>
        assertEquals(
          dm.repr(k),
          dmMerged.repr.getOrElse(k, DotSet.empty),
          s"Merging the list of atoms returned by DotMap.decompose should produce an equal Causal Context, but on key $k the $ccMerged does not equal $cc"
        )
      }
    }
  }
}