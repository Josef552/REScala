package clangast.expr.binaryop

import clangast.expr.CExpr
import clangast.traversal.CASTMapper

import scala.quoted.{Expr, Quotes}

case class CMinusExpr(lhs: CExpr, rhs: CExpr) extends CBinaryOperator {
  val opcode = "-"

  override def toExpr(using Quotes): Expr[CMinusExpr] = {
    val lhsExpr = lhs.toExpr
    val rhsExpr = rhs.toExpr

    '{ CMinusExpr($lhsExpr, $rhsExpr) }
  }

  override def mapChildren(mapper: CASTMapper): CMinusExpr =
    CMinusExpr(mapper.mapCExpr(lhs), mapper.mapCExpr(rhs))
}
