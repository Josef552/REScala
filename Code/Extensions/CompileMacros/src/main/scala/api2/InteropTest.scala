package rescala.api2

import rescala.default.*
import StandardBundle.CompileGraph

import scala.io.StdIn.readLine

object InteropTest extends App {
  val toC = Evt[Int]()

  val toCString = Evt[String]()

  val remote = CompileGraph.withIO("interopTest")(toC, toCString) { (fromScala, fromScalaString) =>
    import StandardBundle.*

    val localSource = Event(Some(5))

    val localMap = localSource.map(_ / 2)

    val strCopy = Event(fromScalaString.value)

    val plusOne = fromScala.map(_ + 1)

    val toScala = plusOne.map(_ * 2)

    val even = fromScala.map(_ % 2 == 0)

    toScala.observe(i => println(i))

    (toScala, even)
  }

  val tcpClient = new TCPClientConnector("localhost", 8000)
  tcpClient.connect()
  remote.setConnector(tcpClient)

  val (fromC, fromCBoolean) = remote.eventsFromListen()

  remote.startObserving()

  fromC.observe(i => println("fromC: " + i))
  fromCBoolean.observe(b => println("fromCBoolean: " + b))

  var loopCond = true

  while (loopCond) {
    val str = readLine()

    if (str.equals("exit")) {
      loopCond = false
    } else {
      str.toIntOption match {
        case Some(i) =>
          toC.fire(i)
        case None => println("Parse Error")
      }
    }
  }

  tcpClient.closeConnection()
}
