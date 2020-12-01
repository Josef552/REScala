package rescala.extra.invariant

import rescala.core.ReSource

sealed trait InvariantException extends RuntimeException

case class InvariantViolationException(
    t: Throwable,
    reactive: ReSource[SimpleStruct],
    causalErrorChains: Seq[Seq[ReSource[SimpleStruct]]]
) extends InvariantException {

  override val getMessage: String = {
    val chainErrorMessage =
      if (causalErrorChains.nonEmpty)
        "The error was caused by these update chains:\n\n" ++ causalErrorChains.map(_.map(r =>
          s"${r.name.str} with value: ${r.state.value}"
        ).mkString("\n↓\n")).mkString("\n---\n")
      else "The error was not triggered by a change."

    s"${t.getMessage} in reactive ${reactive.name.str}\n$chainErrorMessage\n"
  }

  override def fillInStackTrace(): InvariantViolationException = this
}

case class NoGeneratorException(message: String) extends InvariantException {
  override val getMessage: String = message
}
