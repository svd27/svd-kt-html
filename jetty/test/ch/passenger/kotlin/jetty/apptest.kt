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


/**
 * Created by sdju on 09.08.13.
 */
class AppTests {
    test
    fun login() {
        val app = BosorkApp(URN.gen("bosork", "application", "test.bosork.org", "test"), ArrayList<BosorkService>(), DefaultWebAppSessionProvider())
        val wapp = AppServletModule(app, array(JSResource("resources/js/jquery-1.7.2.js", "test/resource"), CSSResource("resources/html/base.css", "test/resource")), File("D:/dev/svd/proj/kotlin/svd-kt-html/Container/web"))
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

        waf.server?.stop()
    }
}