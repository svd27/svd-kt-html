package ch.passenger.kotlin.bosork.vertx

import ch.passenger.kotlin.bosork.vertx.vertx
import ch.passenger.kotlin.bosork.vertx.*
import org.junit.Test as test
import org.vertx.java.core.net.NetSocket

/**
 * Created with IntelliJ IDEA.
 * User: Duric
 * Date: 28.07.13
 * Time: 17:01
 * To change this template use File | Settings | File Templates.
 */

class VertxTcp() {
    test fun tcpCreate() {
        val l : Object = Object()
        val server = vertx(8100, "localhost") {
            tcp {
               handler {
                   (s) ->
                   println("socket: $s")
                   s?.dataHandler {
                       (b) ->

                       println("Buffer: '$b'")
                       synchronized(l) {
                           l.notify()
                       }
                   }
               }
            }

            tcpClient {
                onConnect(8100, "localhost") {
                    socket ->
                    println("client socket $socket")
                    socket?.write("client here!")

                }
            }
            this
        }

        synchronized(l) {
            l.wait(1000)
            server.stop()
        }
    }
}
