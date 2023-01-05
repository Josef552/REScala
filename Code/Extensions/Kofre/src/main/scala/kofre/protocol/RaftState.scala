package kofre.protocol

import kofre.base.Id
import kofre.protocol.RaftState.*
import kofre.base.Lattice

import scala.util.Try

case class RaftState[T](
    participants: Set[Id] = Set.empty,
    leaderVotes: Set[Vote] = Set.empty,
    valueProposals: Set[Propose[T]] = Set.empty[Propose[T]]
) {
  val consensusSize: Int = (participants.size + 2) / 2

  val (
    currentTerm: Int,
    maxTerm: Int,
    leader: Id
  ) = {
    val grouped = leaderVotes.groupBy(v => (v.term, v.leader))
    val (cterm, cleader) = Try {
      grouped.iterator
        .map { case (k, v) => (k, v.size) }
        .filter(_._2 >= consensusSize)
        .map(_._1)
        .max
    }.getOrElse((0, Id.zero))

    val mterm = Try {
      grouped.iterator
        .map(_._1._1)
        .max
    }.getOrElse(0)
    (cterm, mterm, cleader)
  }

  val nextProposal: Int = Try { valueProposals.iterator.map(_.pos).max + 1 }.getOrElse(0)

  def compress: RaftState[T] = copy(leaderVotes = leaderVotes.filter(_.term >= currentTerm))

  def supportProposalDelta(me: Id): RaftState[T] = {
    val voted = valueProposals
      .filter(proposal => proposal.term == currentTerm && proposal.voter == leader)
      .map(proposal => proposal.copy(voter = me))
    RaftState[T](valueProposals = voted)
  }

  def supportProposal(me: Id): RaftState[T] = Lattice.merge(this, supportProposalDelta(me))

  def proposeDelta(me: Id, value: T): RaftState[T] = {
    if (me != leader) RaftState[T]()
    else {
      RaftState[T](valueProposals = Set(Propose(currentTerm, me, nextProposal, value)))
    }
  }

  def propose(me: Id, value: T): RaftState[T] =
    Lattice.merge(this, proposeDelta(me, value))

  def becomeCandidateDelta(me: Id): RaftState[T] =
    RaftState[T](leaderVotes = Set(Vote(maxTerm + 1, me, me)))

  def becomeCandidate(me: Id): RaftState[T] =
    Lattice.merge(this, becomeCandidateDelta(me))

  def supportLeaderDelta(me: Id): RaftState[T] = {
    val votes = leaderVotes.filter(candidate => candidate.term == maxTerm)
    if (votes.exists(_.voter == me)) RaftState[T]()
    else {
      if (votes.isEmpty) RaftState[T]()
      else {
        val bestCandidate = votes.groupBy(_.leader).maxBy(_._2.size)._1
        RaftState[T](leaderVotes = Set(Vote(maxTerm, bestCandidate, me)))
      }
    }
  }

  def supportLeader(me: Id): RaftState[T] =
    Lattice.merge(this, supportLeaderDelta(me))

  lazy val byRound: IndexedSeq[Set[Propose[T]]] = {
    val grouped = valueProposals.groupBy(_.pos)
    Range(0, nextProposal).map { pos => grouped.getOrElse(pos, Set.empty) }
  }

  // TODO: this still has an issue when votes for old terms come in
  lazy val values: List[T] = {
    def decision(proposals: Set[Propose[T]]): Option[Option[T]] = {
      val (size, value) =
        proposals.groupBy(_.value).map(g => (g._2.size, Some(g._1))).maxByOption(_._1).getOrElse((0, None))
      if (size >= consensusSize) Some(value)
      else {
        val term = proposals.headOption.fold(-1)(_.term)
        if (term != currentTerm) DecisionImpossible
        else {
          val undecided = participants.size - proposals.size
          if (undecided + size >= consensusSize) Undecided
          else {
            DecisionImpossible
          }
        }
      }
    }
    valueProposals
      .groupBy(p => p.pos)
      .iterator
      .map(g => (g._1, decision(g._2)))
      .toList.sortBy(_._1)
      .iterator.map(_._2)
      .takeWhile(_.isDefined)
      .flatten.flatten.toList

  }
}

object RaftState {

  private val DecisionImpossible = Some(None)
  private val Undecided          = None

  case class Vote(term: Int, leader: Id, voter: Id)
  case class Propose[T](term: Int, voter: Id, pos: Int, value: T)

  implicit def raftLatticeInstance[T]: Lattice[RaftState[T]] =
    new Lattice[RaftState[T]] {
      override def merge(left: RaftState[T], right: RaftState[T]): RaftState[T] = {
        RaftState(
          Lattice.merge(left.participants, right.participants),
          Lattice.merge(left.leaderVotes, right.leaderVotes),
          Lattice.merge(left.valueProposals, right.valueProposals)
        )
      }
    }

}
