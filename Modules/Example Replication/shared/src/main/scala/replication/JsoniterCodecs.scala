package replication

import com.github.plokhotnyuk.jsoniter_scala.core.{JsonKeyCodec, JsonReader, JsonValueCodec, JsonWriter}
import com.github.plokhotnyuk.jsoniter_scala.macros.{CodecMakerConfig, JsonCodecMaker}
import rdts.base.Uid
import rdts.datatypes.alternatives.ResettableCounter
import rdts.datatypes.contextual.{ReplicatedSet, EnableWinsFlag, MultiVersionRegister, ObserveRemoveMap, ReplicatedList}
import rdts.datatypes.{
  Epoch, GrowOnlyCounter, GrowOnlyList, GrowOnlyMap, GrowOnlySet, LastWriterWins, PosNegCounter, TwoPhaseSet
}
import rdts.dotted.Dotted
import rdts.datatypes.experiments.AuctionInterface.AuctionData
import rdts.time.{ArrayRanges, Dot, Dots, Time}

object JsoniterCodecs {

  def bimapCodec[A, B](codec: JsonValueCodec[A], to: A => B, from: B => A): JsonValueCodec[B] = new JsonValueCodec[B]:
    override def decodeValue(in: JsonReader, default: B): B = to(codec.decodeValue(in, from(default)))
    override def encodeValue(x: B, out: JsonWriter): Unit   = codec.encodeValue(from(x), out)
    override def nullValue: B                               = to(codec.nullValue)

  /** Causal Context */
  implicit val arrayOfLongCodec: JsonValueCodec[Array[Time]] = JsonCodecMaker.make

  implicit val arrayRangesCodec: JsonValueCodec[ArrayRanges] = bimapCodec(
    arrayOfLongCodec,
    ar => new ArrayRanges(ar, ar.length),
    x => x.inner.slice(0, x.used)
  )

  implicit val idKeyCodec: JsonKeyCodec[rdts.base.Uid] = new JsonKeyCodec[Uid]:
    override def decodeKey(in: JsonReader): Uid           = Uid.predefined(in.readKeyAsString())
    override def encodeKey(x: Uid, out: JsonWriter): Unit = out.writeKey(Uid.unwrap(x))
  implicit val CausalContextCodec: JsonValueCodec[Dots] = JsonCodecMaker.make

  /** AddWinsSet */

  implicit def AWSetStateCodec[E: JsonValueCodec]: JsonValueCodec[ReplicatedSet[E]] =
    JsonCodecMaker.make(CodecMakerConfig.withMapAsArray(true))

  given JsonValueCodec[Uid] = bimapCodec(
    JsonCodecMaker.make[String],
    Uid.predefined,
    Uid.unwrap
  )
  implicit def AWSetEmbeddedCodec[E: JsonValueCodec]: JsonValueCodec[Map[E, Set[Dot]]] =
    JsonCodecMaker.make(CodecMakerConfig.withMapAsArray(true))

  /** EWFlag */

  // implicit def EWFlagStateCodec: JsonValueCodec[Dotted[Dots]] = JsonCodecMaker.make
  implicit def EWFlagStateCodec: JsonValueCodec[EnableWinsFlag] = JsonCodecMaker.make

  // implicit def EWFlagEmbeddedCodec: JsonValueCodec[Set[Dot]] = JsonCodecMaker.make

  /** GCounter */
  implicit def MapStringIntStateCodec: JsonValueCodec[Map[String, Int]] = JsonCodecMaker.make
  implicit def GCounterStateCodec: JsonValueCodec[GrowOnlyCounter]      = JsonCodecMaker.make

  /** GrowOnlyList */
  implicit def growOnlyListCodec[E: JsonValueCodec]: JsonValueCodec[GrowOnlyList[E]] =
    JsonCodecMaker.make(CodecMakerConfig.withMapAsArray(true))

  /** GrowOnlySet */
  implicit def GSetStateCodec[E: JsonValueCodec]: JsonValueCodec[GrowOnlySet[E]] = JsonCodecMaker.make

  /** LastWriterWins */
  implicit def LastWriterWinsStateCodec[A: JsonValueCodec]: JsonValueCodec[Dotted[Map[Dot, LastWriterWins[A]]]] =
    JsonCodecMaker.make(CodecMakerConfig.withMapAsArray(true))

  implicit def LastWriterWinsEmbeddedCodec[A: JsonValueCodec]: JsonValueCodec[Map[Dot, LastWriterWins[A]]] =
    JsonCodecMaker.make(CodecMakerConfig.withMapAsArray(true))

  /** MultiVersionRegister */

  implicit def MVRegisterEmbeddedCodec[A: JsonValueCodec]: JsonValueCodec[MultiVersionRegister[A]] =
    JsonCodecMaker.make(CodecMakerConfig.withMapAsArray(true))

  /** ObserveRemoveMap */
  implicit def ORMapStateCodec[K: JsonValueCodec, V: JsonValueCodec]: JsonValueCodec[ObserveRemoveMap[K, V]] =
    JsonCodecMaker.make(CodecMakerConfig.withMapAsArray(true))

  /** PNCounter */

  implicit def PNCounterStateCodec: JsonValueCodec[PosNegCounter] = JsonCodecMaker.make

  /** ResettableCounter */

  implicit def RCounterStateCodec: JsonValueCodec[ResettableCounter] =
    JsonCodecMaker.make(CodecMakerConfig.withMapAsArray(true))

  /** RGA */
  implicit def RGAStateCodec[E: JsonValueCodec]: JsonValueCodec[Dotted[ReplicatedList[E]]] = {
    JsonCodecMaker.make(CodecMakerConfig.withMapAsArray(true))
  }

  /** Rubis */

  implicit def RubisStateCodec: JsonValueCodec[(
      Dotted[Map[(String, String), Set[Dot]]],
      Map[String, String],
      Map[String, AuctionData]
  )] =
    JsonCodecMaker.make(CodecMakerConfig.withMapAsArray(true))

  /** TwoPhaseSet */

  implicit def TwoPSetStateCodec[E: JsonValueCodec]: JsonValueCodec[TwoPhaseSet[E]] = JsonCodecMaker.make

  implicit def withContextWrapper[E: JsonValueCodec]: JsonValueCodec[Dotted[E]] = JsonCodecMaker.make

  implicit def twoPSetContext[E: JsonValueCodec]: JsonValueCodec[Dotted[TwoPhaseSet[E]]] =
    withContextWrapper(using TwoPSetStateCodec)

  implicit def spcecificCodec: JsonValueCodec[Dotted[GrowOnlyMap[Int, ReplicatedSet[Int]]]] =
    JsonCodecMaker.make[Dotted[Map[Int, ReplicatedSet[Int]]]].asInstanceOf

}
