package rescala.core.infiltration

import rescala.core
import rescala.core.{Scheduler, Struct}
import rescala.levelbased.LevelState

/** Accesses private[rescala] values for some low level tests */
object Infiltrator {
  //final def getLevel[S <: LevelStruct](reactive: graph.Reactive[S])(implicit maybe: CreationTicket[S]) = maybe {t => reactive.state.level(t.turn)}
  final def assertLevel[S <: Struct](
      reactive: core.ReSource[S],
      level: Int,
      text: String = "level did not match"
  )(implicit maybe: Scheduler[S]) =
    reactive.state match {
      case rb: LevelState[_, S @unchecked] => {
        val rblevel = maybe.forceNewTransaction() { at =>
          rb.level()
        }
        assert(rblevel == level, s"$text, $reactive level was $rblevel but expected $level")
      }
      case _ =>
    }
}
