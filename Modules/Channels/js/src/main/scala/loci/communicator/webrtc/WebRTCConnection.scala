package loci.communicator.webrtc

import channel.MesageBufferExtensions.asArrayBuffer
import channel.{InChan, JsArrayBufferMessageBuffer, MessageBuffer, OutChan, Prod}
import de.rmgk.delay.{Async, Sync}
import org.scalajs.dom
import org.scalajs.dom.RTCDataChannelState

import scala.concurrent.duration.DurationInt
import scala.scalajs.js
import scala.scalajs.js.typedarray.ArrayBuffer

class WebRTCReceiveFailed(message: String)    extends Exception(message)
class WebRTCConnectionFailed(message: String) extends Exception(message)

class WebRTCConnection(channel: dom.RTCDataChannel) extends InChan with OutChan {
  override def receive: Prod[MessageBuffer] = Async.fromCallback {
    channel.onmessage = { (event: dom.MessageEvent) =>
      event.data match {
        case data: ArrayBuffer =>
          Async.handler.succeed(new JsArrayBufferMessageBuffer(data))

        case data: dom.Blob =>
          val reader = new dom.FileReader
          reader.onerror = { (event) =>
            Async.handler.fail(WebRTCReceiveFailed(s"reading message from blob returned error, event: $event"))
          }
          reader.onload = { (event: dom.Event) =>
            val data = event.target.asInstanceOf[js.Dynamic].result.asInstanceOf[ArrayBuffer]
            Async.handler.succeed(new JsArrayBufferMessageBuffer(data))
          }
          reader.readAsArrayBuffer(data)

        case other =>
          println(s"--------------------")
          println(s"received some message that is neither an array buffer nor a blob, but printlns to:")
          println(other)
          println(s"--------------------")
      }
    }

    channel.onerror = { (evt: dom.Event) =>
      Async.handler.fail(WebRTCReceiveFailed(s"channel error: $evt"))
    }

    channel.onclose = { (evt: dom.Event) =>
      Async.handler.fail(WebRTCReceiveFailed(s"channel closed: $evt"))
    }

    channel.readyState match
      case RTCDataChannelState.closed => Async.handler.fail(WebRTCReceiveFailed(s"channel already closed"))
      case _                          =>

  }
  override def send(message: MessageBuffer): Async[Any, Unit] =
    Sync(channel.send(message.asArrayBuffer))
}

object WebRTCConnection {
  def open(channel: dom.RTCDataChannel): Async[Any, WebRTCConnection] = Async.fromCallback {
    channel.readyState match {
      case dom.RTCDataChannelState.connecting =>
        // strange fix for strange issue with Chromium
        // val handle = js.timers.setTimeout(1.day) { channel.readyState; () }

        channel.onopen = { (_: dom.Event) =>
          // js.timers.clearTimeout(handle)
          Async.handler.succeed(new WebRTCConnection(channel))
        }

      case dom.RTCDataChannelState.open =>
        Async.handler.succeed(new WebRTCConnection(channel))

      case dom.RTCDataChannelState.closing | dom.RTCDataChannelState.closed =>
        Async.handler.fail(new WebRTCConnectionFailed("channel closed"))
    }
  }

}
