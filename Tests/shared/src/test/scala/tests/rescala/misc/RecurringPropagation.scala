package tests.rescala.misc

import tests.rescala.testtools.RETests

class RecurringPropagation extends RETests { multiEngined { engine => import engine._

  test("can start propagations in observers") {

    val e1 = Evt[Int]
    val m1 = e1.map(_ + 10)
    val v1 = Var(2)

    m1.observe(v1.set)

    assert(v1.now == 2)

    e1.fire(100)
    assert(v1.now == 110)

  }

  test("recursive propagations") {

    val e1 = Evt[Int]
    val m1 = e1.map(_ + 10)
    val v1 = Var(2)

    m1.observe(v1.set)

    assert(v1.now == 2)

    v1.observe(current => if (current > 100) () else e1.fire(current))

    assert(v1.now == 102)

  }

} }
