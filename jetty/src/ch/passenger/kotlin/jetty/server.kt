package ch.passenger.kotlin.jetty

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.Connector
import org.eclipse.jetty.server.AbstractNetworkConnector
import org.eclipse.jetty.server.Handler
import org.eclipse.jetty.servlet.ServletContextHandler
import javax.servlet.Servlet
import org.eclipse.jetty.websocket.servlet.WebSocketCreator
import org.eclipse.jetty.websocket.servlet.WebSocketServlet
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory
import javax.servlet.http.HttpSession

/**
 * Created by sdju on 25.07.13.
 */

fun jetty(cfg : Server.()->Unit) : Server {
    val server = Server()

    server.cfg()

    return server
}

fun Server.connectors(cfg : Server.() -> Array<Connector>) {
    setConnectors(cfg())
}

fun Server.handlers(cfg : Server.() -> Handler) {
    setHandler(cfg())
}


fun ServletContextHandler.servlets(cfg : ServletContextHandler.() -> Map<String,Class<Servlet?>>) {
    cfg().entrySet().forEach { addServlet(it.value, it.key) }
}

fun ServletContextHandler.socket(cfg : ServletContextHandler.() -> Pair<String,Class<BosorkWebsocketAdapter>>) {
    val pair = cfg()
    val wsc = WebSocketCreator {
        (req,resp) ->
        val ctor = pair.second.getConstructor(javaClass<HttpSession>())
        ctor.newInstance(req?.getSession())
    }
    class WSSServlet() : BosorkWebsocketServlet(wsc)
    addServlet(javaClass<WSSServlet>() as Class<Servlet>, pair.first)

}




fun AbstractNetworkConnector.configure(cfg : AbstractNetworkConnector.()->Unit) {
    cfg()
}

