package kofre.datatypes

import kofre.base.{Bottom, DecomposeLattice, Defs}
import kofre.datatypes.GrowMap
import kofre.decompose.*
import kofre.decompose.interfaces.MVRegisterInterface.MVRegister
import kofre.datatypes.ObserveRemoveMap
import kofre.dotted.{DotMap, Dotted, DottedDecompose, DottedLattice, HasDots}
import kofre.syntax.{ArdtOpsContains, DottedName, OpsSyntaxHelper}
import kofre.time.{Dot, Dots}

case class ObserveRemoveMap[K, V](inner: DotMap[K, V])

/** An ObserveRemoveMap (Observed-Remove Map) is a Delta CRDT that models a map from an arbitrary key type to nested causal Delta CRDTs.
  * In contrast to [[GrowMap]], ObserveRemoveMap allows the removal of key/value pairs from the map.
  *
  * The nested CRDTs can be queried/mutated by calling the queryKey/mutateKey methods with a DeltaQuery/DeltaMutator generated
  * by a CRDT Interface method of the nested CRDT. For example, to enable a nested EWFlag, one would pass `EWFlagInterface.enable()`
  * as the DeltaMutator to mutateKey.
  */
object ObserveRemoveMap {

  def empty[K, V]: ObserveRemoveMap[K, V] = ObserveRemoveMap(DotMap.empty)

  given bottom[K, V]: Bottom[ObserveRemoveMap[K, V]] = Bottom.derived

  given contextDecompose[K, V: DottedDecompose: HasDots: Bottom]: DottedDecompose[ObserveRemoveMap[K, V]] =
    import DotMap.contextDecompose
    DottedDecompose.derived

  def make[K, V](
      dm: DotMap[K, V] = DotMap.empty[K, V],
      cc: Dots = Dots.empty
  ): Dotted[ObserveRemoveMap[K, V]] = Dotted(ObserveRemoveMap(dm), cc)

  implicit class ORMapSyntax[C, K, V](container: C)(using ArdtOpsContains[C, ObserveRemoveMap[K, V]])
      extends OpsSyntaxHelper[C, ObserveRemoveMap[K, V]](container) {

    def contains(k: K)(using QueryP): Boolean = current.contains(k)

    def queryKey[A](k: K)(using QueryP, CausalP, Bottom[V]): Dotted[V] = {
      Dotted(current.inner.getOrElse(k, Bottom[V].empty), context)
    }

    def queryAllEntries(using QueryP): Iterable[V] = current.inner.values
    def mutateKey(k: K, m: (Defs.Id, Dotted[V]) => Dotted[V])(using
        CausalMutationP,
        IdentifierP,
        Bottom[V]
    ): C = {
      val v = current.inner.getOrElse(k, Bottom[V].empty)

      m(replicaID, context.wrap(v)) match {
        case Dotted(stateDelta, ccDelta) =>
          make[K, V](
            dm = DotMap(Map(k -> stateDelta)),
            cc = ccDelta
          ).mutator
      }
    }

    def mutateKeyNamedCtx(k: K)(m: DottedName[V] => DottedName[V])(using
        CausalMutationP,
        IdentifierP,
        Bottom[V]
    ): C = {
      val v                           = current.inner.getOrElse(k, Bottom[V].empty)
      val Dotted(stateDelta, ccDelta) = m(DottedName(replicaID, Dotted(v, context))).anon
      make[K, V](
        dm = DotMap(Map(k -> stateDelta)),
        cc = ccDelta
      ).mutator
    }

    def remove(k: K)(using CausalMutationP, Bottom[V], HasDots[V]): C = {
      val v = current.inner.getOrElse(k, Bottom[V].empty)

      make[K, V](
        cc = HasDots[V].dots(v)
      ).mutator
    }

    def removeAll(keys: Iterable[K])(using CausalMutationP, Bottom[V], HasDots[V]): C = {
      val values = keys.map(k => current.inner.getOrElse(k, Bottom[V].empty))
      val dots = values.foldLeft(Dots.empty) {
        case (set, v) => set union HasDots[V].dots(v)
      }

      make(
        cc = dots
      ).mutator
    }

    def removeByValue(cond: Dotted[V] => Boolean)(using CausalMutationP, DottedDecompose[V], HasDots[V]): C = {
      val toRemove = current.inner.values.collect {
        case v if cond(Dotted(v, context)) => HasDots[V].dots(v)
      }.fold(Dots.empty)(_ union _)

      make(
        cc = toRemove
      ).mutator
    }

    def clear()(using CausalMutationP, DottedDecompose[V], HasDots[V]): C = {
      make(
        cc = current.inner.dots
      ).mutator
    }
  }
}
