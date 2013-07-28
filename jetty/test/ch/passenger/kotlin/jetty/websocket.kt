package ch.passenger.kotlin.jetty

import org.junit.Test as test
import java.net.URI
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest
import org.eclipse.jetty.websocket.api.annotations.WebSocket as webSocket
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect as wsconnect
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose as wsclose
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage as wsmsg
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError as wserror
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.websocket.api.Session
import javax.servlet.http.HttpSession
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.websocket.client.WebSocketClient
import kotlin.test.assertEquals
import java.util.concurrent.TimeUnit
import kotlin.test.assertTrue
import org.eclipse.jetty.websocket.api.WebSocketListener

/**
 * Created with IntelliJ IDEA.
 * User: Duric
 * Date: 28.07.13
 * Time: 06:20
 */
class SocketTest {
    test fun testBareSocket() {
        var connected = false
        var closed = false
        val server = jetty() {
            connectors {
                val c0  = ServerConnector(this)

                c0.configure {
                    setPort(2709)
                }

                array(c0)
            }

            handlers {
                val h = object : ServletContextHandler() {}
                h.socket {
                    class Adaptor(s : HttpSession) : BosorkWebsocketAdapter(s) {

                        public override fun onWebSocketConnect(sess: Session?) {
                            connected = true
                        }


                        public override fun onWebSocketClose(statusCode: Int, reason: String?) {
                            closed = true
                        }


                        public override fun onWebSocketText(message: String?) {
                            getRemote()?.sendString(message?.toUpperCase())
                        }
                    }
                    Pair("/events", javaClass<Adaptor>() as Class<BosorkWebsocketAdapter>)
                }

                h
            }
        }

        server.start()

        try {
            val c = WebSocketClient()
            c.start()
            val uri = URI("ws://localhost:2709/events")
            val req = ClientUpgradeRequest()
            val socket = object : WebSocketListener {
                public override fun onWebSocketBinary(p0: ByteArray?, p1: Int, p2: Int) {
                    throw UnsupportedOperationException()
                }
                public override fun onWebSocketClose(p0: Int, p1: String?) {
                    println("close")
                }
                public override fun onWebSocketConnect(session: Session?) {
                    val future = session?.getRemote()?.sendStringByFuture("a")
                    assertEquals("A", future?.get(2, TimeUnit.SECONDS))
                    synchronized(c) {
                      c.notify()
                    }
                }
                public override fun onWebSocketError(p0: Throwable?) {
                    p0?.printStackTrace()
                }
                public override fun onWebSocketText(p0: String?) {
                    println("got $p0")
                }
            }

            c.connect(socket, uri, req)
            synchronized(c) {
                c.wait()
                c.stop()
                assertTrue(connected)
                assertTrue(closed)
            }
        } finally {
            server.stop()
        }
    }

}

open class IntHolder(val i:Int)

class VerifyError {
    test fun instance() {
        public class X public () : IntHolder(5)

        try {
            val jc = javaClass<X>()
            val x = jc.newInstance()
            println(x.i)
        } catch(e: Exception) {
            e.printStackTrace()
        }
    }

    test fun ctor() {
        public class X public () : IntHolder(5)

        try {
            val jc = javaClass<X>()
            val x = jc.getConstructor()
            println(x.newInstance()?.i)
        } catch(e: Exception) {
            e.printStackTrace()
        }
    }
}