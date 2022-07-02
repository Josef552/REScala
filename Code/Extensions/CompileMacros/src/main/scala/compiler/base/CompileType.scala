package compiler.base

import clangast.expr.{CCharacterLiteral, CDoubleLiteral, CExpr, CFalseLiteral, CFloatLiteral, CIntegerLiteral, CLongLiteral}
import clangast.stubs.StdBoolH
import clangast.types.*
import compiler.context.TranslationContext
import compiler.CompilerCascade

import scala.quoted.*

object CompileType extends TypePC {
  override def compileTypeRepr(using Quotes)(using ctx: TranslationContext, cascade: CompilerCascade):
    PartialFunction[quotes.reflect.TypeRepr, CType] = {
      import quotes.reflect.*

      {
        case ConstantType(_: BooleanConstant) => StdBoolH.bool
        case tpe if tpe =:= TypeRepr.of[Boolean] => StdBoolH.bool
        case ConstantType(_: ByteConstant) => CCharType
        case tpe if tpe =:= TypeRepr.of[Byte] => CCharType
        case ConstantType(_: CharConstant) => CCharType
        case tpe if tpe =:= TypeRepr.of[Char] => CCharType
        case ConstantType(_: ShortConstant) => CShortType
        case tpe if tpe =:= TypeRepr.of[Short] => CShortType
        case ConstantType(_: IntConstant) => CIntegerType
        case tpe if tpe =:= TypeRepr.of[Int] => CIntegerType
        case ConstantType(_: LongConstant) => CLongType
        case tpe if tpe =:= TypeRepr.of[Long] => CLongType
        case ConstantType(_: FloatConstant) => CFloatType
        case tpe if tpe =:= TypeRepr.of[Float] => CFloatType
        case ConstantType(_: DoubleConstant) => CDoubleType
        case tpe if tpe =:= TypeRepr.of[Double] => CDoubleType
        case ConstantType(_: UnitConstant) => CVoidType
        case tpe if tpe =:= TypeRepr.of[Unit] => CVoidType
        case MethodType(_, _, tpe) => cascade.dispatch(_.compileTypeRepr)(tpe)
      }
    }

  def isNumberType(using Quotes)(tpe: quotes.reflect.TypeRepr): Boolean = {
    import quotes.reflect.*

    tpe <:< TypeRepr.of[Byte] ||
      tpe <:< TypeRepr.of[Short] ||
      tpe <:< TypeRepr.of[Char] ||
      tpe <:< TypeRepr.of[Int] ||
      tpe <:< TypeRepr.of[Long] ||
      tpe <:< TypeRepr.of[Float] ||
      tpe <:< TypeRepr.of[Double]
  }

  override def typeName(using Quotes)(using ctx: TranslationContext, cascade: CompilerCascade):
    PartialFunction[quotes.reflect.TypeRepr, String] = {
      import quotes.reflect.*
    
      {
        case MethodType(_, _, tpe) => cascade.dispatch(_.typeName)(tpe)
        case tpe => tpe.typeSymbol.name
      }
    }

  override def classTypeName(using Quotes)(using ctx: TranslationContext, cascade: CompilerCascade):
    PartialFunction[quotes.reflect.TypeRepr, String] = tpe => {
      import quotes.reflect.*
    
      val className = tpe.classSymbol.get.name

      tpe.widen match {
        case AppliedType(_, typeArgs) =>
          val typeArgNames = typeArgs.map(cascade.dispatch(_.typeName))
          className + "_" + typeArgNames.mkString("_")
        case _ => className
      }
    }

  override def defaultValue(using Quotes)(using ctx: TranslationContext, cascade: CompilerCascade):
    PartialFunction[quotes.reflect.TypeRepr, CExpr] = {
      import quotes.reflect.*

      {
        case tpe if tpe =:= TypeRepr.of[Boolean] => CFalseLiteral
        case tpe if tpe =:= TypeRepr.of[Byte] => CCharacterLiteral(0)
        case tpe if tpe =:= TypeRepr.of[Char] => CCharacterLiteral(0)
        case tpe if tpe =:= TypeRepr.of[Short] => CIntegerLiteral(0)
        case tpe if tpe =:= TypeRepr.of[Int] => CIntegerLiteral(0)
        case tpe if tpe =:= TypeRepr.of[Long] => CLongLiteral(0)
        case tpe if tpe =:= TypeRepr.of[Float] => CFloatLiteral(0)
        case tpe if tpe =:= TypeRepr.of[Double] => CDoubleLiteral(0)
        case MethodType(_, _, tpe) => cascade.dispatch(_.defaultValue)(tpe)
      }
    }
  
  def hasDefaultValue(using Quotes)(tpe: quotes.reflect.TypeRepr)(using ctx: TranslationContext, cascade: CompilerCascade): Boolean =
    cascade.dispatchLifted(_.defaultValue)(tpe).isDefined

  def typeArgs(using Quotes): PartialFunction[quotes.reflect.TypeRepr, List[quotes.reflect.TypeRepr]] = tpe => {
    import quotes.reflect.*

    tpe match {
      case AnnotatedType(AppliedType(_, typeArgs), _) => typeArgs
      case MethodType(_, _, AppliedType(_, typeArgs)) => typeArgs
      case AppliedType(_, typeArgs) => typeArgs
      case _ => throw new MatchError(tpe.show(using Printer.TypeReprStructure))
    }
  }
}
