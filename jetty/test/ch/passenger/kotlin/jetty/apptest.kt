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


/**
 * Created by sdju on 09.08.13.
 */
class AppTests {
    test
    fun login() {
        val om = ObjectMapper()!!
        val app = BosorkApp(URN.gen("bosork", "application", "test.bosork.org", "test"), ArrayList<ServiceProvider>(),
                DefaultWebAppSessionFactoryProvider(), AnonymousAuthService.provider)

        val wapp = AppServletModule(app, array(JSResource("resources/js/jquery-1.7.2.js", "test/resource"),
                CSSResource("resources/html/base.css", "test/resource")),
                File("D:/dev/svd/proj/kotlin/svd-kt-html/Container/web"))

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
}