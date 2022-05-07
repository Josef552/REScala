package kofre.protocol

import kofre.decompose.*
import kofre.decompose.interfaces.AWSetInterface
import kofre.decompose.interfaces.AWSetInterface.AWSetSyntax
import kofre.decompose.interfaces.EWFlagInterface.EWFlag
import kofre.protocol.AuctionInterface
import kofre.protocol.AuctionInterface.Bid.User
import kofre.protocol.RubisInterface.{AID, UserAsUIJDLattice}
import kofre.syntax.{AllPermissionsCtx, OpsSyntaxHelper}

/** A Rubis (Rice University Bidding System) is a Delta CRDT modeling an auction system.
  *
  * Bids can only be placed on auctions that were previously opened and with a previously registered userId. When an auction
  * is closed, concurrently placed bids are still accepted and may thus change the winner of the auction. To prevent two
  * replicas from concurrently registering the same userId, requests for registering a new userId must be resolved by a
  * central replica using resolveRegisterUser.
  *
  * This auction system was in part modeled after the Rice University Bidding System (RUBiS) proposed by Cecchet et al. in
  * "Performance and Scalability of EJB Applications", see [[https://www.researchgate.net/publication/2534515_Performance_and_Scalability_of_EJB_Applications here]]
  */
object RubisInterface {
  type AID = String

  type State = (AWSetInterface.AWSet[(User, String)], Map[User, String], Map[AID, AuctionInterface.AuctionData])

  trait RubisCompanion {
    type State = RubisInterface.State

    implicit val UserAsUIJDLattice: DecomposeLattice[User] = RubisInterface.UserAsUIJDLattice
  }

  implicit val UserAsUIJDLattice: DecomposeLattice[User] = DecomposeLattice.AtomicUIJDLattice[User]

  private class DeltaStateFactory {
    val bottom: State = DecomposeLattice[State].empty

    def make(
        userRequests: AWSetInterface.AWSet[(User, String)] = bottom._1,
        users: Map[User, String] = bottom._2,
        auctions: Map[AID, AuctionInterface.AuctionData] = bottom._3
    ): State = (userRequests, users, auctions)
  }

  private def deltaState: DeltaStateFactory = new DeltaStateFactory

  implicit class RubisSyntax[C](container: C) extends OpsSyntaxHelper[C, State](container) {

    def placeBid(auctionId: AID, userId: User, price: Int)(using MutationIDP): C = {
      val (_, users, m) = current
      val newMap =
        if (users.get(userId).contains(replicaID) && m.contains(auctionId)) {
          m.updatedWith(auctionId) { _.map(a => a.bid(userId, price)) }
        } else Map.empty[AID, AuctionInterface.AuctionData]

      deltaState.make(auctions = newMap)
    }

    def closeAuction(auctionId: AID)(using MutationIDP): C = {
      val (_, _, m) = current
      val newMap =
        if (m.contains(auctionId)) {
          m.updatedWith(auctionId) { _.map(a => a.close()) }
        } else Map.empty[AID, AuctionInterface.AuctionData]

      deltaState.make(auctions = newMap)
    }

    def openAuction(auctionId: AID)(using MutationIDP): C = {
      val (_, _, m) = current
      val newMap =
        if (m.contains(auctionId)) Map.empty[AID, AuctionInterface.AuctionData]
        else Map(auctionId -> DecomposeLattice[AuctionInterface.AuctionData].empty)

      deltaState.make(auctions = newMap)
    }

    def requestRegisterUser(userId: User)(using MutationIDP): C = {
      val (req, users, _) = current
      if (users.contains(userId)) deltaState.make()
      else deltaState.make(userRequests = req.add(userId -> replicaID)(using AllPermissionsCtx.withID(replicaID)))
    }

    def resolveRegisterUser()(using MutationIDP): C = {
      val (req, users, _) = current
      val newUsers = req.elements.foldLeft(Map.empty[User, String]) {
        case (newlyRegistered, (uid, rid)) =>
          if ((users ++ newlyRegistered).contains(uid))
            newlyRegistered
          else {
            newlyRegistered.updated(uid, rid)
          }
      }

      deltaState.make(
        userRequests = req.clear(),
        users = newUsers
      )
    }
  }
}