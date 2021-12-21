package rescala.macros

import rescala.core.Core

import scala.annotation.compileTimeOnly

trait ReadableMacroBundle extends Core {
  trait ReadableMacro[+A] extends Readable[A] with MacroAccess[A, Readable[A]]
}

trait MacroAccess[+A, +T] {

  /** Makes the enclosing reactive expression depend on the current value of the reactive.
    * Is an alias for [[value]].
    *
    * @group accessor
    * @see value
    */
  @compileTimeOnly(s"${this} apply can only be used inside of reactive expressions")
  final def apply(): A = throw new IllegalAccessException(s"$this.apply called outside of macro")

  /** Makes the enclosing reactive expression depend on the current value of the reactive.
    * Is an alias for [[rescala.macros.MacroAccess.apply]].
    *
    * @group accessor
    * @see apply
    */
  @compileTimeOnly("value can only be used inside of reactive expressions")
  final def value: A = throw new IllegalAccessException(s"$this.value called outside of macro")

  def resource: T

}
