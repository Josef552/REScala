package rescala.extra.debuggable

case class NodeID(str: String) extends AnyVal

trait DebuggerInterface {
  def saveSnap(snapshotid: String): Unit
  def saveNode(id: NodeID, name: String, value: String): Unit
  def saveEdge(from: NodeID, to: NodeID): Unit
  def sourceHint(id: NodeID, hint: String, values: Seq[String]): Unit
}

object DisableDebugging extends DebuggerInterface {
  override def saveNode(id: NodeID, name: String, value: String): Unit         = ()
  override def saveEdge(from: NodeID, to: NodeID): Unit                        = ()
  override def saveSnap(snapshotid: String): Unit                              = ()
  override def sourceHint(id: NodeID, hint: String, values: Seq[String]): Unit = ()
}
