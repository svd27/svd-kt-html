package ch.passenger.kotlin.html.servlet;

import javax.servlet.Servlet
import org.eclipse.jetty.websocket.api.annotations.WebSocket as webSocket
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect as onConnect
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose as onClose
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError as onError
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage as onMsg

import org.eclipse.jetty.websocket.api.Session



/**
 * Created by sdju on 19.07.13.
 */

webSocket()
public class EventServer {
    private var session : Session? = null
     {
         println("im here")
     }

    public onConnect final fun onConnect(s : Session) {
        session = s
        println(session?.getUpgradeRequest()?.getSession())
        s.getRemote()?.sendStringByFuture("connected")
    }

    public onClose final fun onClose(status : Int, reason : String) {
        session = null
    }

    public onMsg final fun msg(msg : String) {
        println(msg)
    }

}
