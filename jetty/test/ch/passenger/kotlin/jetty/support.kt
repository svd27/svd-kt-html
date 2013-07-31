package ch.passenger.kotlin.jetty

import com.google.inject.Singleton
import com.google.inject.Inject
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import com.google.inject.AbstractModule
import com.google.inject.servlet.ServletModule

/**
 * Created by sdju on 25.07.13.
 */
[Singleton]
[Inject] public class  TestServlet public [Inject]() : HttpServlet() {
    protected override fun service(req: HttpServletRequest?, resp: HttpServletResponse?) {
        if(resp==null) throw IllegalStateException()

        val writer = resp.getWriter()

        if(writer==null) throw IllegalStateException()
        resp.setContentType("text/plain;charset=utf-8")
        writer.write(textResp)
        writer.flush()
        writer.close()
    }

    class object {
        public val textResp : String = "Hi Kotlin!"
    }
}

trait WebAppModule  {
    public val modules : Array<AbstractModule>
}

class SM [Inject]() : ServletModule() {
    protected override fun configureServlets() {
        bind(javaClass<TestServlet>())!!.asEagerSingleton()
        serve("/*")!!.with(javaClass<TestServlet>())
    }
}

class WA1() : WebAppModule {
    override public val modules : Array<AbstractModule> = array(SM())
}