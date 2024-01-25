package replication.papoctokens

import kofre.base.{Bottom, Lattice, Uid}
import kofre.datatypes.contextual.ReplicatedSet
import kofre.datatypes.contextual.ReplicatedSet.syntax
import kofre.datatypes.experiments.RaftState
import kofre.dotted.Dotted
import kofre.syntax.{DeltaBuffer, OpsSyntaxHelper, ReplicaId}

import scala.util.Random

case class Token(epoche: Long, owner: Uid)

object Token {
  given Lattice[Token] = Lattice.fromOrdering(Ordering.by(_.epoche))

  given bottom: Bottom[Token] = Bottom.provide(Token(Long.MinValue, Uid.zero))

  def unchanged: Token = bottom.empty
}

case class TokenAgreement(
    token: Token,
    wants: ReplicatedSet[Uid]
)
object TokenAgreement {

  val unchanged: TokenAgreement = TokenAgreement(Token.unchanged, ReplicatedSet.empty)

  extension [C, E](container: C)
    def tokens: syntax[C] = syntax(container)

  implicit class syntax[C](container: C) extends OpsSyntaxHelper[C, TokenAgreement](container) {

    def updateWant(f: Dotted[ReplicatedSet[Uid]] => Dotted[ReplicatedSet[Uid]]): CausalMutate = mutate:
      val addWant: Dotted[ReplicatedSet[Uid]] = f(current.wants.inheritContext)

      addWant.map: aw =>
        TokenAgreement(Token.unchanged, aw)

    def request(using ReplicaId): CausalMutate = updateWant(_.add(replicaId))

    def release(using ReplicaId): CausalMutate = updateWant(_.remove(replicaId))

    def grant(using ReplicaId): Mutate = mutate:
      if !isOwner then unchanged
      else
        // We find the “largest” ID that wants the token.
        // This is incredibly “unfair” but does prevent deadlocks in case someone needs multiple tokens.
        current.wants.elements.maxOption match
          case Some(head) if head != replicaId =>
            TokenAgreement(Token(current.token.epoche + 1, head), ReplicatedSet.empty)
          case _ => unchanged

    def isOwner(using ReplicaId, PermQuery): Boolean = replicaId == current.token.owner
  }
}

case class ExampleTokens(
    calendarA: TokenAgreement,
    calendarB: TokenAgreement
)
