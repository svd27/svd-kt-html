package ch.passenger.kotlin.jetty

import org.eclipse.jetty.websocket.api.WebSocketAdapter
import javax.servlet.http.HttpSession
import org.eclipse.jetty.websocket.servlet.WebSocketCreator
import org.eclipse.jetty.websocket.servlet.WebSocketServlet
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory

/**
 * Created with IntelliJ IDEA.
 * User: Duric
 * Date: 28.07.13
 * Time: 05:12
 */

abstract class BosorkWebsocketAdapter(protected val session : HttpSession?) : WebSocketAdapter()

abstract class BosorkWebsocketServlet(val creator : WebSocketCreator) : WebSocketServlet() {

    public override fun configure(p0: WebSocketServletFactory?) {
        p0?.setCreator(creator)
    }
}