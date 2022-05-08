package kofre.syntax

import kofre.base.Defs.Id
import kofre.base.{DecomposeLattice, Defs, Lattice}
import kofre.causality.CausalContext
import kofre.contextual.WithContext
import scala.annotation.implicitNotFound

/** The basic idea behind this machinery is to allow lattices of type L to be stored in a Container of type C.
  * In the simplest case C = L and the lattice is used as is.
  * More complex containers contain additional information such as the replica ID
  * or a set of deltas since the last synchronization.
  * No matter the concrete container, they should all offer the same API to the underlying lattice.
  */

@implicitNotFound("Unsure how to extract »${L}«\nfrom »${C}")
trait PermQuery[C, L]:
  def query(c: C): L
  def focus[M](q: L => M): PermQuery[C, M] = (c: C) => q(PermQuery.this.query(c))
trait PermMutate[C, L] extends PermQuery[C, L]:
  def mutate(c: C, delta: L): C
@implicitNotFound(
  "Requires a replica ID.\nWhich seems unavailable in »${C}«\nMissing a container?"
  )
trait PermId[C]:
  def replicaId(c: C): Id
class FixedId[C](id: Id) extends PermId[C]:
  override def replicaId(c: C): Id = id
@implicitNotFound(
  "Requires causal context permission.\nNo context in »${C}«\nMissing a container?"
  )
trait PermCausal[C]:
  def context(c: C): CausalContext
@implicitNotFound(
  "Requires context mutation permission.\nUnsure how to extract context from »${C}«\nto modify »${L}«"
)
trait PermCausalMutate[C, L]:
  def mutateContext(container: C, withContext: WithContext[L]): C
trait PermIdMutate[C, L] extends PermId[C], PermMutate[C, L]

object PermQuery:
  given identityQuery[A]: PermQuery[A, A] = PermMutate.identityDeltaMutate
object PermMutate:
  given identityDeltaMutate[A]: PermMutate[A, A] with
    override def query(c: A): A            = c
    override def mutate(c: A, delta: A): A = delta
object PermIdMutate:
  def withID[C, L](id: Id)(using mctx: PermMutate[C, L]): PermIdMutate[C, L] = new PermIdMutate[C, L]:
    def mutate(c: C, delta: L): C = mctx.mutate(c, delta)
    def replicaId(c: C): Id       = id
    def query(c: C): L            = mctx.query(c)

/** Helper trait to state that container C contains lattices of type L.
  * This is used for better type inference
  */
@implicitNotFound("Could not show that »${C}\ncontains ${L}")
trait ArdtOpsContains[C, L]
object ArdtOpsContains:
  given identityContains[L]: ArdtOpsContains[L, L] with {}
  given transitiveContains[A, B, C](using ArdtOpsContains[A, B], ArdtOpsContains[B, C]): ArdtOpsContains[A, C] with {}

/** Helps to define operations that update any container [[C]] containing values of type [[L]]
  * using a scheme where mutations return deltas which are systematically applied.
  */
trait OpsSyntaxHelper[C, L](container: C) {
  final type MutationIDP    = PermIdMutate[C, L]
  final type QueryP         = PermQuery[C, L]
  final type MutationP      = PermMutate[C, L]
  final type IdentifierP    = PermId[C]
  final type CausalP        = PermCausal[C]
  final type CausalMutation = PermCausalMutate[C, L]

  final type MutationID = MutationIDP ?=> C
  final type Mutation   = MutationP ?=> C
  final type Query[T]   = QueryP ?=> T

  final protected def current(using perm: QueryP): L                    = perm.query(container)
  final protected def replicaID(using perm: IdentifierP): Defs.Id       = perm.replicaId(container)
  final protected given mutate(using perm: MutationP): Conversion[L, C] = perm.mutate(container, _)
  final protected def context(using perm: CausalP): CausalContext       = perm.context(container)
  final protected given causalMutate(using perm: CausalMutation): Conversion[WithContext[L], C] =
    perm.mutateContext(container, _)

}
