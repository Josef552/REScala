package replication.papoctokens

import kofre.base.{Bottom, Lattice, Orderings, Uid}
import kofre.datatypes.Epoche
import kofre.datatypes.contextual.ReplicatedSet
import kofre.datatypes.contextual.ReplicatedSet.syntax
import kofre.datatypes.experiments.RaftState
import kofre.dotted.Dotted
import kofre.syntax.ReplicaId.replicaId
import kofre.syntax.{DeltaBuffer, OpsSyntaxHelper, ReplicaId}
import kofre.time.Dots

import scala.util.Random

case class Ownership(epoche: Long, owner: Uid)

object Ownership {
  given Lattice[Ownership] = Lattice.fromOrdering(Orderings.lexicographic)

  given bottom: Bottom[Ownership] = Bottom.provide(Ownership(Long.MinValue, Uid.zero))

  def unchanged: Ownership = bottom.empty
}

case class Token(os: Ownership, wants: ReplicatedSet[Uid]) {
  def updateWants(dottedWant: Dotted[ReplicatedSet[Uid]]): Dotted[Token] =
    dottedWant.map: wants =>
      Token(Ownership.unchanged, wants)

  def request(using ReplicaId, Dots): Dotted[Token] = updateWants:
    wants.addElem(replicaId)

  def release(using ReplicaId, Dots): Dotted[Token] = updateWants:
    wants.removeElem(replicaId)

  def grant(using ReplicaId): Token =
    if !isOwner then Token.unchanged
    else
      selectFrom(wants) match
        case None => Token.unchanged
        case Some(nextOwner) =>
          Token(Ownership(os.epoche + 1, nextOwner), ReplicatedSet.empty)

  def selectFrom(wants: ReplicatedSet[Uid])(using ReplicaId) =
    // We find the “largest” ID that wants the token.
    // This is incredibly “unfair” but does prevent deadlocks in case someone needs multiple tokens.
    wants.elements.maxOption.filter(id => id != replicaId)

  def isOwner(using ReplicaId): Boolean = replicaId == os.owner
}

object Token {
  val unchanged: Token = Token(Ownership.unchanged, ReplicatedSet.empty)
  given Lattice[Token] = Lattice.derived
}

case class ExampleTokens(
    calendarAinteractionA: Token,
    calendarBinteractionA: Token
)

case class Vote(owner: Uid, voter: Uid)
case class Voting(rounds: Epoche[ReplicatedSet[Vote]]) {

  def request(using ReplicaId, Dots): Dotted[Voting] =
    if !rounds.value.isEmpty then Voting.unchanged
    else voteFor(replicaId)

  def voteFor(uid: Uid)(using ReplicaId, Dots): Dotted[Voting] =
    val newVote = rounds.value.addElem(Vote(uid, replicaId))
    newVote.map(rs => Voting(rounds.write(rs)))

  def release(using ReplicaId): Voting =
    Voting(Epoche(rounds.counter + 1, ReplicatedSet.empty))

  def vote(using ReplicaId, Dots): Dotted[Voting] =
    val (id, count) = leadingCount
    if checkIfMajorityPossible(count)
    then voteFor(id)
    else Dotted(release)

  def checkIfMajorityPossible(count: Int): Boolean =
    val totalVotes     = rounds.value.elements.size
    val remainingVotes = Voting.participants - totalVotes
    (count + remainingVotes) > Voting.threshold

  def leadingCount(using ReplicaId): (Uid, Int) =
    val votes: Set[Vote] = rounds.value.elements
    votes.groupBy(_.owner).map((o, elems) => (o, elems.size)).maxBy((o, size) => size)

  def isOwner(using ReplicaId): Boolean =
    val (id, count) = leadingCount
    id == replicaId && count >= Voting.threshold

}

object Voting {

  val unchanged: Dotted[Voting] = Dotted(Voting(Epoche.empty))

  given Lattice[Voting] = Lattice.derived

  val participants = 5
  val threshold    = (participants / 2) + 1

}