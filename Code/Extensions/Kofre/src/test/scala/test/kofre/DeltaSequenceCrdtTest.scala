package test.kofre

import kofre.base.Lattice
import kofre.deprecated.containers.DeltaBufferRDT
import kofre.dotted.Dotted
import kofre.rga.{DeltaSequence, Vertex}

class DeltaSequenceCrdtTest extends munit.FunSuite {

  test("basic interaction") {
    val ds      = DeltaBufferRDT.empty("", DeltaSequence.empty[String])
    val a       = "a"
    val vertex1 = Vertex.fresh()
    val vertex2 = Vertex.fresh()
    val added2: DeltaBufferRDT[DeltaSequence[String]] = ds
      .addRightDelta(a, Vertex.start, vertex1, "Hello world!")
      .addRightDelta(a, Vertex.start, vertex2, "Hello world 2!")
    assertEquals(added2.iterator.toList, List("Hello world 2!", "Hello world!"), "add right is correctly ordered")

    val combined = added2.addRightDelta(a, vertex2, Vertex.fresh(), "Hello world after 2!")

    assertEquals(combined.iterator.toList, List("Hello world 2!", "Hello world after 2!", "Hello world!"))

    val combined2 = added2.addRightDelta(a, vertex2, Vertex.fresh(), "Hello world after 2 the second!")

    assertEquals(combined2.iterator.toList, List("Hello world 2!", "Hello world after 2 the second!", "Hello world!"))

  }

}
