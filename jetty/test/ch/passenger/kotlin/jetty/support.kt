package ch.passenger.kotlin.jetty

import com.google.inject.Singleton
import com.google.inject.Inject
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

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