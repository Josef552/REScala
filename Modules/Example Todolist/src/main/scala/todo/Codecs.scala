package todo

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonKeyCodec, JsonReader, JsonValueCodec, JsonWriter}
import com.github.plokhotnyuk.jsoniter_scala.macros.{CodecMakerConfig, JsonCodecMaker}
import kofre.base.Uid
import kofre.base.Uid.asId
import kofre.datatypes.LastWriterWins.TimedVal
import kofre.datatypes.{CausalLastWriterWins, ReplicatedList}
import kofre.dotted.{DotFun, Dotted}
import kofre.syntax.DeltaBuffer
import kofre.time.Dot
import loci.transmitter.IdenticallyTransmittable
import rescala.extra.replication.DeltaFor
import todo.Todolist.replicaId

import scala.annotation.nowarn

object Codecs {

  implicit val taskRefCodec: JsonValueCodec[TaskRef] = JsonCodecMaker.make
  implicit val dotKeyCodec: JsonKeyCodec[Dot] = new JsonKeyCodec[Dot] {
    override def decodeKey(in: JsonReader): Dot = {
      val Array(time, id) = in.readKeyAsString().split("-", 2)
      Dot(Uid.predefined(id), time.toLong)
    }
    override def encodeKey(x: Dot, out: JsonWriter): Unit = out.writeKey(s"${x.time}-${x.replicaId}")
  }
  implicit val idCodec: JsonValueCodec[Uid] = JsonCodecMaker.make[String].asInstanceOf
  implicit val idKeyCodec: JsonKeyCodec[kofre.base.Uid] = new JsonKeyCodec[Uid]:
    override def decodeKey(in: JsonReader): Uid           = Uid.predefined(in.readKeyAsString())
    override def encodeKey(x: Uid, out: JsonWriter): Unit = out.writeKey(Uid.unwrap(x))

  @nowarn()
  implicit val codecState: JsonValueCodec[Dotted[ReplicatedList[TaskRef]]] =
    JsonCodecMaker.make(CodecMakerConfig.withMapAsArray(true))
  implicit val codecRGA: JsonValueCodec[DeltaBuffer[Dotted[ReplicatedList[TaskRef]]]] =
    new JsonValueCodec[DeltaBuffer[Dotted[ReplicatedList[TaskRef]]]] {
      override def decodeValue(
          in: JsonReader,
          default: DeltaBuffer[Dotted[ReplicatedList[TaskRef]]]
      ): DeltaBuffer[Dotted[ReplicatedList[TaskRef]]] = {
        val state = codecState.decodeValue(in, default.state)
        DeltaBuffer[Dotted[ReplicatedList[TaskRef]]](state)
      }
      override def encodeValue(x: DeltaBuffer[Dotted[ReplicatedList[TaskRef]]], out: JsonWriter): Unit =
        codecState.encodeValue(x.state, out)
      override def nullValue: DeltaBuffer[Dotted[ReplicatedList[TaskRef]]] =
        DeltaBuffer(Dotted(ReplicatedList.empty[TaskRef]))
    }

  implicit val transmittableList: IdenticallyTransmittable[DeltaFor[ReplicatedList[TaskRef]]] =
    IdenticallyTransmittable()
  implicit val codectDeltaForTasklist: JsonValueCodec[DeltaFor[ReplicatedList[TaskRef]]] = JsonCodecMaker.make

  implicit val codecLwwState: JsonValueCodec[Dotted[DotFun[TimedVal[TaskData]]]] = JsonCodecMaker.make

  implicit val codecDeltaForLWW: JsonValueCodec[DeltaFor[CausalLastWriterWins[TaskData]]] = JsonCodecMaker.make

  implicit val transmittableDeltaForLWW: IdenticallyTransmittable[DeltaFor[CausalLastWriterWins[TaskData]]] =
    IdenticallyTransmittable()

  implicit val codecLww: JsonValueCodec[DeltaBuffer[Dotted[CausalLastWriterWins[TaskData]]]] = JsonCodecMaker.make

}