package decentral

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import kofre.contextual.WithContext
import kofre.predef.AddWinsSet
import loci.registry.Binding
import loci.serializer.jsoniterScala._
import loci.transmitter.IdenticallyTransmittable
import rescala.extra.lattices.delta.JsoniterCodecs._

import scala.concurrent.Future

object Bindings {
  type SetState = WithContext[AddWinsSet[Int]]

  case class CheckpointMessage(cp: Checkpoint, changes: SetState)

  implicit val intCodec: JsonValueCodec[Int] = JsonCodecMaker.make

  implicit val checkpointCodec: JsonValueCodec[Checkpoint] = JsonCodecMaker.make

  implicit val setStateMessageCodec: JsonValueCodec[SetState] = JsonCodecMaker.make

  implicit val checkpointMessageCodec: JsonValueCodec[CheckpointMessage] = JsonCodecMaker.make

  implicit val transmittableSetState: IdenticallyTransmittable[SetState] = IdenticallyTransmittable()

  implicit val transmittableCheckpointMessage: IdenticallyTransmittable[CheckpointMessage] = IdenticallyTransmittable()

  val receiveDeltaBinding: Binding[SetState => Unit, SetState => Future[Unit]] =
    Binding[SetState => Unit]("receiveDelta")

  val getCheckpointsBinding: Binding[() => Map[String, Int], () => Future[Map[String, Int]]] =
    Binding[() => Map[String, Int]]("getCheckpoints")

  val receiveCheckpointBinding: Binding[CheckpointMessage => Unit, CheckpointMessage => Future[Unit]] =
    Binding[CheckpointMessage => Unit]("receiveCheckpoint")
}
