package ch.passenger.kotlin.html.servlet;

import org.eclipse.jetty.websocket.servlet.WebSocketServlet
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory
import org.eclipse.jetty.websocket.api.WebSocketAdapter
import javax.servlet.Servlet
import org.eclipse.jetty.websocket.api.Session
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.eclipse.jetty.websocket.servlet.WebSocketCreator
import org.eclipse.jetty.websocket.api.UpgradeRequest
import org.eclipse.jetty.websocket.api.UpgradeResponse
import javax.servlet.http.HttpSession
import ch.passenger.kotlin.basis.URN


public class BosorkWebSocketServlet : WebSocketServlet(), Servlet {

    public override fun configure(p0: WebSocketServletFactory?) {
        p0?.setCreator(BosorkWebSocketCreator())
    }


    protected override fun service(request: HttpServletRequest?, response: HttpServletResponse?) {
        super<WebSocketServlet>.service(request, response)
    }
}

public class BosorkWebSocketCreator : WebSocketCreator {

    public override fun createWebSocket(req: UpgradeRequest?, resp: UpgradeResponse?): Any? {
        val session : HttpSession? = req?.getSession() as HttpSession
        if(session==null)
            return null

        if(session.getAttribute(LoginHandler.TOKEN_KEY)==null)
            return null

        return BosorkEventsServer(session)
    }
}

public class BosorkEventsServer(private val session : HttpSession) : WebSocketAdapter() {
    private val token : URN = session.getAttribute(LoginHandler.TOKEN_KEY) as URN
    public override fun onWebSocketText(message: String?) {
        println("!!!!SVD!!! $message")
        sendFut("I know You ${token.urn}")
    }

    public fun sendFut(msg : String) {
        getRemote()?.sendStringByFuture(msg)
    }

    public override fun onWebSocketConnect(sess: Session?) {
        super<WebSocketAdapter>.onWebSocketConnect(sess)
    }
    public override fun onWebSocketError(cause: Throwable?) {
        super<WebSocketAdapter>.onWebSocketError(cause)
    }


}