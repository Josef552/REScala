package kofre.protocol

import kofre.base.DecomposeLattice
import kofre.decompose.*
import kofre.decompose.interfaces.GSetInterface
import kofre.decompose.interfaces.GSetInterface.GSetSyntax
import kofre.protocol.AuctionInterface.Bid.User
import kofre.syntax.OpsSyntaxHelper

object AuctionInterface {
  sealed trait Status
  case object Open   extends Status
  case object Closed extends Status

  object Status {
    implicit val StatusAsUIJDLattice: DecomposeLattice[Status] = new DecomposeLattice[Status] {
      override def lteq(left: Status, right: Status): Boolean = (left, right) match {
        case (Closed, Open) => false
        case _              => true
      }

      override def decompose(state: Status): Iterable[Status] = List(state)

      override def empty: Status = Open

      override def merge(left: Status, right: Status): Status = (left, right) match {
        case (Open, Open) => Open
        case _            => Closed
      }
    }
  }

  case class Bid(userId: User, bid: Int)

  case object Bid {
    type User = String
  }

  case class AuctionData(
                          bids: Set[Bid] = DecomposeLattice[Set[Bid]].empty,
                          status: Status = DecomposeLattice[Status].empty,
                          winner: Option[User] = None
  )

  case object AuctionData {
    implicit val AuctionDataAsUIJDLattice: DecomposeLattice[AuctionData] = new DecomposeLattice[AuctionData] {
      override def lteq(left: AuctionData, right: AuctionData): Boolean = (left, right) match {
        case (AuctionData(lb, ls, _), AuctionData(rb, rs, _)) =>
          DecomposeLattice[Set[Bid]].lteq(lb, rb) && DecomposeLattice[Status].lteq(ls, rs)
      }

      override def decompose(state: AuctionData): Iterable[AuctionData] =
        state match {
          case AuctionData(bids, status, _) =>
            bids.map(b =>
              AuctionData(bids = DecomposeLattice[Set[Bid]].empty.insert(b))
            ) ++ (status match {
              case Open   => Set()
              case Closed => Set(AuctionData(status = Closed))
            })
        }

      override def empty: AuctionData = AuctionData()

      override def merge(left: AuctionData, right: AuctionData): AuctionData = (left, right) match {
        case (AuctionData(lb, ls, _), AuctionData(rb, rs, _)) =>
          val bidsMerged   = DecomposeLattice[Set[Bid]].merge(lb, rb)
          val statusMerged = DecomposeLattice[Status].merge(ls, rs)
          val winnerMerged = statusMerged match {
            case Open   => None
            case Closed => bidsMerged.maxByOption(_.bid).map(_.userId)
          }

          AuctionData(bidsMerged, statusMerged, winnerMerged)
      }
    }
  }

  implicit class AuctionSyntax[C](container: C) extends OpsSyntaxHelper[C, AuctionData](container) {
    def bid(userId: User, price: Int)(using MutationP): C =
      AuctionData(bids = current.bids.insert(Bid(userId, price))).mutator

    def close()(using MutationP): C = AuctionData(status = Closed).mutator
  }
}
