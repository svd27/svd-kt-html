package ch.passenger.kotlin.jetty

import ch.passenger.kotlin.basis.BosorkApp
import ch.passenger.kotlin.basis.URN
import java.util.ArrayList
import java.io.File
import org.apache.http.client.HttpClient
import org.apache.http.impl.client.DefaultHttpClient
import org.junit.Test as test
import org.junit.Ignore as ignore
import ch.passenger.kotlin.basis.BosorkService
import ch.passenger.kotlin.basis.AnonymousAuthService
import ch.passenger.kotlin.basis.ServiceProvider
import org.eclipse.jetty.websocket.client.WebSocketClient
import org.eclipse.jetty.websocket.api.annotations.*
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.WebSocketListener
import java.net.URI
import org.apache.http.HttpEntity
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.StringReader
import java.io.BufferedReader
import java.io.InputStreamReader
import ch.passenger.kotlin.jetty.support.EchoService
import ch.passenger.kotlin.jetty.support.EchoServiceProvider
import org.apache.http.client.methods.HttpPut
import org.apache.http.entity.StringEntity
import ch.passenger.kotlin.json.Jsonifier
import ch.passenger.kotlin.jetty.support.EchoRequest


/**
 * Created by sdju on 09.08.13.
 */
class AppTests {
    test
    fun login() {
        val om = ObjectMapper()!!
        val app = BosorkApp(URN.gen("bosork", "application", "test.bosork.org", "test"), ArrayList<ServiceProvider>(),
                DefaultWebAppSessionFactoryProvider(), AnonymousAuthService.provider)

        val wapp = AppServletModule(app, listOf(JSResource("resources/js/jquery-1.7.2.js", "test/resource"),
                LinkResource.css("resources/html/base.css", "test/resource")),
                File("D:/dev/svd/proj/kotlin/svd-kt-html/Container/web"), ArrayList())

        val waf = AppFactory(wapp, 2709)
        waf.init()
        waf.server?.start()

        val client : HttpClient = DefaultHttpClient();
        val init = org.apache.http.client.methods.HttpGet("http://localhost:2709/test")
        var response = client.execute(init)!!
        println("${response.getStatusLine()?.getStatusCode()}: ${response.getStatusLine()?.getReasonPhrase()}")
        println("${response.getEntity()?.writeTo(System.out)}")

        response = client.execute(init)!!
        println("${response.getStatusLine()?.getStatusCode()}: ${response.getStatusLine()?.getReasonPhrase()}")
        println("${response.getEntity()?.writeTo(System.out)}")

        val login = org.apache.http.client.methods.HttpPost("http://localhost:2709/test/login")
        val loginRequest = om.createObjectNode()!!
        loginRequest.put("user", "test")
        loginRequest.put("pwd", "test")
        loginRequest.put("cid", 0.toInt())
        val sli = om.writerWithDefaultPrettyPrinter()!!.writeValueAsString(loginRequest)!!
        login.setEntity(org.apache.http.entity.StringEntity(sli))
        response = client.execute(login)!!
        println("${response.getStatusLine()?.getStatusCode()}: ${response.getStatusLine()?.getReasonPhrase()}")
        val lie = response.getEntity()
        val sos = BufferedReader(InputStreamReader(lie?.getContent()!!))
        val linode = om.readTree(sos)!!


        val wsc = WebSocketClient()
        val events = [WebSocket] object  : WebSocketListener {
            var session : Session? = null
            [OnWebSocketConnect]
            fun connected(s: Session) {

            }
            public override fun onWebSocketBinary(payload: ByteArray?, offset: Int, len: Int) {
                println("binary!?")
            }
            public override fun onWebSocketClose(statusCode: Int, reason: String?) {
                println("closed on $statusCode: $reason")
            }
            public override fun onWebSocketConnect(session: Session?) {
                println("connected on ${session}")
                this.session = session
            }
            public override fun onWebSocketError(cause: Throwable?) {
                println("BAD: ${cause?.getMessage()}")
                cause?.printStackTrace()
            }
            public override fun onWebSocketText(message: String?) {
                println("COOL: '$message'")
            }
        }

        val token = linode!!.path("payload")!!.path("token")?.textValue()!!
        val urntok = URN(token)
        val uri = URI.create("ws://localhost:2709/test/responses?${urntok.specifier}")
        wsc.start()
        wsc.connect(events, uri)

        synchronized(wsc) {
            wsc.wait(5000.toLong())
        }


        waf.server?.stop()
    }

    test
    fun service() {
        val om = ObjectMapper()!!
        val respLock = Object()
        val app = BosorkApp(URN.gen("bosork", "application", "test.bosork.org", "test"), listOf(EchoServiceProvider()),
                DefaultWebAppSessionFactoryProvider(), AnonymousAuthService.provider)

        val wapp = AppServletModule(app, listOf(JSResource("resources/js/jquery-1.7.2.js", "test/resource"),
                LinkResource.css("resources/html/base.css", "test/resource")),
                File("D:/dev/svd/proj/kotlin/svd-kt-html/Container/web"), ArrayList())

        val waf = AppFactory(wapp, 2709)
        waf.init()
        waf.server?.start()

        val client : HttpClient = DefaultHttpClient();
        val init = org.apache.http.client.methods.HttpGet("http://localhost:2709/test")
        val respInit = client.execute(init)!!
        println("${respInit.getStatusLine()?.getStatusCode()}: ${respInit.getStatusLine()?.getReasonPhrase()}")
        println("${respInit.getEntity()?.writeTo(System.out)}")


        val login = org.apache.http.client.methods.HttpPost("http://localhost:2709/test/login")
        val loginRequest = om.createObjectNode()!!
        loginRequest.put("user", "test")
        loginRequest.put("pwd", "test")
        loginRequest.put("cid", 0.toInt())
        val sli = om.writerWithDefaultPrettyPrinter()!!.writeValueAsString(loginRequest)!!
        login.setEntity(org.apache.http.entity.StringEntity(sli))
        val response = client.execute(login)!!
        val lie = response.getEntity()
        val sos = BufferedReader(InputStreamReader(lie?.getContent()!!))
        val linode = om.readTree(sos)!!


        val wscLock = Object()
        val wsc = WebSocketClient()
        wsc.start()
        val responses = [WebSocket] object  : WebSocketListener {
            var session : Session? = null
            public override fun onWebSocketBinary(payload: ByteArray?, offset: Int, len: Int) {
                println("binary!?")
            }
            public override fun onWebSocketClose(statusCode: Int, reason: String?) {
                println("closed on $statusCode: $reason")
            }
            public override fun onWebSocketConnect(session: Session?) {
                println("REQUESTER connected on ${session}")
                this.session = session
                synchronized(wscLock) {
                    wscLock.notifyAll()
                }
            }
            public override fun onWebSocketError(cause: Throwable?) {
                println("BAD: ${cause?.getMessage()}")
                cause?.printStackTrace()
            }
            public override fun onWebSocketText(message: String?) {
                println("COOL: '$message'")
                synchronized(respLock) {
                    respLock.notifyAll()
                }
            }
        }

        val events = [WebSocket] object  : WebSocketListener {
            var session : Session? = null
            public override fun onWebSocketBinary(payload: ByteArray?, offset: Int, len: Int) {
                println("binary!?")
            }
            public override fun onWebSocketClose(statusCode: Int, reason: String?) {
                println("closed on $statusCode: $reason")
            }
            public override fun onWebSocketConnect(session: Session?) {
                println("EVENTER connected on ${session}")
                this.session = session
                synchronized(wscLock) {
                    wscLock.notifyAll()
                }
            }
            public override fun onWebSocketError(cause: Throwable?) {
                println("BAD: ${cause?.getMessage()}")
                cause?.printStackTrace()
            }
            public override fun onWebSocketText(message: String?) {
                println("EVENT: '$message'")
                synchronized(wscLock) {
                    wscLock.notifyAll()
                }
            }
        }


        val token = linode!!.path("payload")!!.path("token")?.textValue()!!
        val urntok = URN(token)
        val uri = URI.create("ws://localhost:2709/test/responses?${urntok.specifier}")
        synchronized(wscLock) {
            wsc.connect(responses, uri)
            wscLock.wait(1000)
        }

        val uriev = URI.create("ws://localhost:2709/test/events?${urntok.specifier}")
        val wscev = WebSocketClient()
        wscev.start()
        synchronized(wscLock) {
            wscev.connect(events, uriev)
            wscLock.wait(1000)
        }



        val echoReq = om.createObjectNode()!!
        val expect = "svd"
        echoReq.put("token", token)
        echoReq.put("kotlin-type", javaClass<EchoRequest>().getName())
        echoReq.put("service", EchoService.ME.urn)
        echoReq.put("cid", 1.toInt())
        echoReq.put("wahwah", 2.toInt())
        echoReq.put("events", 3.toInt())
        echoReq.put("echo", expect)

        val put = HttpPut("http://localhost:2709/test/echo")
        put.setEntity(StringEntity(Jsonifier.asString(echoReq)))
        client.execute(put)

        synchronized(respLock) {
            respLock.wait(100)
        }

        synchronized(wsc) {
            wsc.wait(5000.toLong())
        }


        waf.server?.stop()
    }

}