package loci
package compatibility

object either {
  @inline private[loci] def left[L, R](either: Either[L, R]) =
    either.left.toOption.get

  @inline private[loci] def right[L, R](either: Either[L, R]) =
    either.right.toOption.get

  @inline private[loci] def map[L, R, RO](either: Either[L, R])(f: R => RO) =
    either.right map f

  @inline private[loci] def flatMap[L, R, LO >: L, RO](either: Either[L, R])(f: R => Either[LO, RO]) =
    either.right flatMap f

  @inline private[loci] def foreach[L, R, T](either: Either[L, R])(f: R => T) =
    either.right foreach f
}
