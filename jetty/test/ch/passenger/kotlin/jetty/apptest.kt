package ch.passenger.kotlin.jetty

import ch.passenger.kotlin.basis.BosorkApp
import ch.passenger.kotlin.basis.URN
import java.util.ArrayList
import java.io.File

/**
 * Created by sdju on 09.08.13.
 */
class AppTests {
    fun login() {
        val app = BosorkApp(URN.gen("bosork", "application", "test.bosork.org", "test"), ArrayList())
        val wapp = AppServletModule(app, array(JSResource("resources/js/jquery-1.7.2.js", "test/resource"), CSSResource("resources/html/base.css", "test/resource")), File("D:/dev/svd/proj/kotlin/svd-kt-html/Container/web"))
        val waf = AppFactory(wapp, 2709)
        waf.init()
        waf.server?.start()
    }
}