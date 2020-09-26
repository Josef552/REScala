package reswing.millgame.versions.signals

import reswing.millgame._
import reswing.millgame.types._

import rescala.default._

class MillBoard {
  /* wrap stones Var, to have the same interface as other versions */
  def stones = stonesVar.now

  /* spiral-indexed board slots, starting innermost lower left, going clockwise */
  val stonesVar: Var[Vector[Slot]] = Var(Vector.fill(24)(Empty)) //#VAR

  /* slots by the 16 lines of the game */
  val lines = Signal { //#SIG
    MillBoard.lines map { _ map { slot => stonesVar()(slot.index) } }
  }

  /* lines mapped to owners */
  val lineOwners: Signal[Vector[Slot]] = Signal { //#SIG
    (lines() map { line => //#EF
      if (line forall { _ == line.head }) line.head else Empty
    }).toVector
  }

  /* debug
	lineOwners.changed += { (owner:Vector[Slot]) =>
	  println(owner.mkString(","))
	}
   */

  /* access slot state by index */
  def apply(slot: SlotIndex): Slot = stonesVar.now.apply(slot.index)
  def update(slot: SlotIndex, color: Slot): Unit =
    stonesVar.transform(_.updated(slot.index, color))
  def update(indexColor: Map[SlotIndex, Slot]): Unit =
    stonesVar.set(stonesVar.now.zipWithIndex.map({
      case (color, i) => indexColor.getOrElse(SlotIndex(i), color)
    }))

  val color: Signal[SlotIndex => Slot] = Signal { //#SIG
    val stones = stonesVar()
    slot => stones(slot.index)
  }

  /* several test signals*/
  val canPlace: Signal[SlotIndex => Boolean] = Signal { //#SIG
    val col = color()
    slot => col(slot) == Empty
  }
  val canRemove: Signal[SlotIndex => Boolean] = Signal { //#SIG
    val col = color()
    slot => col(slot) != Empty
  }
  val canJump: Signal[(SlotIndex, SlotIndex) => Boolean] = Signal { //#SIG
    val removeAllowed = canRemove()
    val placeAllowed  = canPlace()
    (from, to) => removeAllowed(from) && placeAllowed(to)
  }
  val canMove: Signal[(SlotIndex, SlotIndex) => Boolean] = Signal { //#SIG
    val jumpAllowed = canJump()
    (from, to) => jumpAllowed(from, to) && MillBoard.isConnected(from, to)
  }

  def place(slot: SlotIndex, color: Slot) = this(slot) = color

  def remove(slot: SlotIndex) = this(slot) = Empty

  def move(from: SlotIndex, to: SlotIndex) = {
    /// NOTE: this is an interesting detail in the signal version
    /// If we delete the new stone FIRST, we might have < 3 stones,
    /// which gets propagated and triggers the end of the game!
    /// But otherwise we have an additional stone on the board that
    /// may created a mill where none should exist.
    /// "move" actually is an atomic operation on the board, and we probably want events
    /// to trigger fine-grained changes like this
    /// This is why we need an additional update member, that can perform
    /// multiple updates atomically.

    this() = Map(from -> Empty, to -> this(from))
  }

  val possibleMoves: Signal[Seq[(SlotIndex, SlotIndex)]] = Signal { //#SIG
    MillBoard.indices flatMap { from =>
      MillBoard.indices collect { case to if canMove()(from, to) => from -> to }
    }
  }

  val possibleJumps: Signal[Seq[(SlotIndex, SlotIndex)]] = Signal { //#SIG
    MillBoard.indices flatMap { from =>
      MillBoard.indices collect { case to if canJump()(from, to) => from -> to }
    }
  }

  /// NOTE: Workaround because change fires even when there is no value change
  val lineOwnersChanged    = lineOwners.change && (c => c._2 != c._1) //#EVT //#IF
  val lineOwnersNotChanged = lineOwners.change.\(lineOwnersChanged)
  lineOwnersNotChanged += { x =>
    println("not changed: " + x)
  }
  val millOpenedOrClosed = lineOwners.change.map { //#EVT //#IF
    change =>
      /// NOTE: Workaround because change event fires (null, new) tuple
      if (change._1 eq null) change._2.find(_ != Empty).get
      else (change._1 zip change._2).collectFirst { case (old, n) if old != n => n }.get
  }

  val millClosed: Event[Slot] = millOpenedOrClosed && { (_: Slot) != Empty } //#EVT

  val numStones: Signal[Slot => Int] = Signal { //#SIG
    val stones = stonesVar()
    (color: Slot) => stones.count(_ == color)
  }
  val blackStones = Signal { numStones()(Black) } //#SIG
  val whiteStones = Signal { numStones()(White) } //#SIG
  val numStonesChanged: Event[(Slot, Int)] = //#EVT
    blackStones.changed.map((Black, _: Int)) || whiteStones.changed.map((White, _: Int)) //#IF //#EF //#IF
}
