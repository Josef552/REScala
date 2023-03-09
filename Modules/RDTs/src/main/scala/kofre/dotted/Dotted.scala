package kofre.dotted

import kofre.base.Lattice.Operators
import kofre.base.{Bottom, Uid, Lattice}
import kofre.dotted.{DotFun, DotSet}
import kofre.syntax.{PermCausalMutate, PermQuery}
import kofre.time.{Dot, Dots}

case class Dotted[A](store: A, context: Dots) {
  def map[B](f: A => B): Dotted[B]      = Dotted(f(store), context)
  def knows(dot: Dot): Boolean          = context.contains(dot)
  def deletions(using HasDots[A]): Dots = context diff contained
  def contained(using HasDots[A]): Dots = store.dots
}

object Dotted {

  def empty[A: Bottom]: Dotted[A] = Dotted(Bottom.empty[A], Dots.empty)
  def apply[A](a: A): Dotted[A]   = Dotted(a, Dots.empty)

  given syntaxPermissions[L](using DottedLattice[L]): PermCausalMutate[Dotted[L], L] with {
    override def mutateContext(c: Dotted[L], delta: Dotted[L]): Dotted[L] = c merge delta
    override def query(c: Dotted[L]): L                                   = c.store
    override def context(c: Dotted[L]): Dots                              = c.context
  }

}
