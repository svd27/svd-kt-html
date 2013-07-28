package ch.passenger.kotlin.bosork.vertx

import org.vertx.java.platform.Container
import org.vertx.java.platform.Verticle
import org.vertx.java.core.Vertx
import org.vertx.java.core.VertxFactory
import org.vertx.java.core.net.NetServer
import org.vertx.java.core.AsyncResultHandler
import org.vertx.java.core.AsyncResult
import org.vertx.java.core.net.NetSocket
import org.vertx.java.core.Handler
import org.vertx.java.core.buffer.Buffer
import org.vertx.java.core.net.NetClient


/**
 * Created with IntelliJ IDEA.
 * User: Duric
 * Date: 28.07.13
 * Time: 16:07
 * To change this template use File | Settings | File Templates.
 */
fun vertx(port : Int, host:String, init : Vertx.() -> Vertx) : Vertx {
    val v = VertxFactory.newVertx(port, "localhost")!!
    v.init()
    return v
}

fun Vertx.tcp(init : NetServer.() -> NetServer) {
    val ns = createNetServer()!!
    ns.init()
}

fun NetServer.onCreate(port:Int, host:String, init: (AsyncResult<NetServer>) -> Unit) {
    val l = AsyncResultHandler<NetServer>() {
        r -> init(r!!)
    }
    listen(port, host, l)
    this
}

fun NetServer.handler(init:Handler<NetSocket>.(NetSocket?)->Unit): NetServer {
    val h = object : Handler<NetSocket> {
        public override fun handle(socket: NetSocket?) {
            init(socket)
        }
    }

    return connectHandler(h)!!
}

fun Handler<NetSocket>.data(handler : (Buffer?)->Unit) {

    this

}

fun Vertx.tcpClient(init: NetClient.() -> NetClient) {
    val c = createNetClient()!!
    c.init()
    c
}

fun NetClient.onConnect(port:Int, host:String, init:Handler<AsyncResult<NetSocket>>.(NetSocket?)->Unit) : NetClient {
    val h = object : Handler<AsyncResult<NetSocket>> {
        public override fun handle(result: AsyncResult<NetSocket>?) {
            init(result?.result())
        }
    }
    connect(port, host, h)

    return this
}





