package clangast.stmt
import scala.quoted.{Expr, Quotes}

case class CDefaultStmt(body: List[CStmt]) extends CSwitchCase {
  override def textgen: String =
    s"""
       |default:
       |${body.map(_.textgen).mkString("\n").indent(2).stripTrailing()}
    """.strip().stripMargin

  override def toExpr(using Quotes): Expr[CDefaultStmt] = {
    val bodyExpr = Expr.ofList(body.map(_.toExpr))

    '{ CDefaultStmt($bodyExpr) }
  }
}
