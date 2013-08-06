package ch.passenger.kotlin.basis

import org.junit.Test as test
import org.junit.Ignore as ignore
import java.util.ArrayList
import net.engio.mbassy.listener.Handler
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import org.slf4j.LoggerFactory

/**
 * Created with IntelliJ IDEA.
 * User: Duric
 * Date: 04.08.13
 * Time: 05:02
 * To change this template use File | Settings | File Templates.
 */

private val testLog = LoggerFactory.getLogger(javaClass<BosorkApp>()?.getPackage()?.getName())!!


class EchoRequest(override val session: BosorkSession, override val service: URN,
                  override val clientId: Long, val echo: String) : BosorkRequest {
    {

    }
}
class EchoResponse(override val session: BosorkSession, override val service: URN,
                   override val clientId: Long, val echo: String) : BosorkResponse {
    {
        testLog.info("created echo: $echo")
    }
}

class EchoService() : AbstractService(EchoServiceProvider.Echo,
        EchoServiceProvider.Echo.specifier) {
    override fun init() {

    }
    override fun destroy() {

    }
    override fun call(req: BosorkRequest): BosorkResponse {
        if(req is EchoRequest)
            return EchoResponse(req.session, id, req.clientId, req.echo)
        return wrongRequest(req, javaClass<EchoRequest>())
    }

}

class DoubleEchoService : BosorkService {
    override val id: URN = EchoServiceProvider.Echo
    override val shortName: String = id.specifier
    override fun init() {

    }
    override fun destroy() {

    }
    override fun call(req: BosorkRequest): BosorkResponse {
        if(req is EchoRequest){
            val sb = StringBuilder()
            req.echo.forEach { sb.append("$it$it") }
            return EchoResponse(req.session, id, req.clientId, sb.toString())
        }
        return wrongRequest(req, javaClass<EchoRequest>())
    }

}

class EchoServiceProvider : ServiceProvider {
    override fun creates(): Iterable<URN> {
        return listOf(EchoServiceProvider.Echo, EchoServiceProvider.DoubleEcho)
    }
    override fun create(service: URN, app: BosorkApp): BosorkService {
        var s : BosorkService? = null
        when(service.urn) {
            EchoServiceProvider.Echo.urn -> s = EchoService()
            EchoServiceProvider.DoubleEcho.urn -> s =  DoubleEchoService()
            else -> throw BosorkServiceNotFound(service)
        }
        return s!!
    }
    override fun createOnStartup(): Iterable<URN> {
        return creates()
    }

    class object {
        val Echo: URN = URN.service("echo", "test.bosork.org")
        val DoubleEcho: URN = URN.service("double-echo", "test.bosork.org")
    }
}

class SimpleAuthProvider : AuthProvider {
    override val login: URN = LOGIN
    override val finisher: URN = LOGOUT
    override fun create(service: URN, app: BosorkApp): BosorkService {
        when(service) {
            login -> return object : BosorkService {
                override val id: URN = login
                override val shortName: String = id.specifier
                override fun init() {

                }
                override fun destroy() {

                }
                override fun call(req: BosorkRequest): BosorkResponse {
                    if(req is LoginRequest) {
                        if (req.session is NullSession) {
                            if(req.user == "guest" && req.pwd == "test") {
                                val s = AbstractSession(URN.token("test"), app)
                                return LoginResponse(s, id, req.clientId, null)
                            }
                            return LoginResponse(req.session, id, req.clientId, BosorkLoginFailed(req.user))
                        } else {
                            return LoginResponse(req.session, id, req.clientId, null)
                        }
                    }

                    wrongRequest(req, javaClass<LoginRequest>())
                }
            }
            finisher -> return object : BosorkService {
                override val id: URN = finisher
                override val shortName: String = id.specifier
                override fun init() {

                }
                override fun destroy() {
                    throw UnsupportedOperationException()
                }
                override fun call(req: BosorkRequest): BosorkResponse {
                    if(req is LogoutRequest) {
                        return LogoutResponse(req.session, finisher, req.clientId)
                    }

                    wrongRequest(req, javaClass<LogoutRequest>())
                }
            }
            else -> throw BosorkServiceNotFound(service)
        }
    }
    class object {
        val LOGIN = URN.service("login", "test.bosork.org")
        val LOGOUT = URN.service("logout", "test.bosork.org")
    }
}

class BosorkAppTests {
    test
            fun simpleLogin() {
        val sap = SimpleAuthProvider()
        val app = BosorkApp(listOf(sap, EchoServiceProvider()))
        app.start()
        var respReceived = false
        var loginSuccess = false
        val lock = Object()

        val l = object : Any() {
            [Handler]
                    fun listen(resp: BosorkResponse) {
                when(resp) {
                    is LoginResponse -> {
                        respReceived = true
                        if(resp.error == null) {
                            loginSuccess = true
                        }
                    }
                    else -> {
                    }
                }
                synchronized(lock) {
                    lock.notifyAll()
                }
            }
        }

        app.listen(l)

        synchronized(lock) {
            app.request(LoginRequest(app, SimpleAuthProvider.LOGIN, 1, "guest", "test"))
            lock.wait()
        }

        assertTrue(respReceived)
        assertTrue(loginSuccess)
    }

    test
    fun simpleEcho() {
        val sap = SimpleAuthProvider()
        val app = BosorkApp(listOf(sap, EchoServiceProvider()))
        app.start()
        var respReceived = false
        var loginSuccess = false
        var session : BosorkSession = NullSession(app)
        val echo = "A"
        var echoed = ""
        val lock = Object()

        val l = object : Any() {
            [Handler]
                    fun listen(resp: BosorkResponse) {
                when(resp) {
                    is LoginResponse -> {
                        respReceived = true
                        if(resp.error == null) {
                            loginSuccess = true
                            session = resp.session
                            session.listen(object : Any() {
                                [Handler]
                                        fun listen(resp: BosorkResponse) {
                                    when(resp) {
                                        is EchoResponse -> {
                                            echoed = resp.echo
                                            synchronized(lock) {
                                                lock.notifyAll()
                                            }
                                        }
                                        else -> {}
                                    }
                                }
                            })
                        }
                    }
                    else -> {
                    }
                }
                synchronized(lock) {
                    lock.notifyAll()
                }
            }
        }

        app.listen(l)

        synchronized(lock) {
            app.request(LoginRequest(app, SimpleAuthProvider.LOGIN, 1, "guest", "test"))
            lock.wait(1000)
        }

        assertTrue(respReceived)
        assertTrue(loginSuccess)

        synchronized(lock) {
            session.request(EchoRequest(session, EchoServiceProvider.Echo, 1, echo))
            lock.wait(1000)
        }

        assertEquals(echo, echoed)

    }

    test
    fun doubleEcho() {
        val sap = SimpleAuthProvider()
        val app = BosorkApp(listOf(sap, EchoServiceProvider()))
        app.start()
        var respReceived = false
        var loginSuccess = false
        var session : BosorkSession = NullSession(app)
        val echo = "ABC"
        val expect = "AABBCC"
        var echoed = ""
        val lock = Object()

        val l = object : Any() {
            [Handler]
                    fun listen(resp: BosorkResponse) {
                when(resp) {
                    is LoginResponse -> {
                        respReceived = true
                        if(resp.error == null) {
                            loginSuccess = true
                            session = resp.session
                            session.listen(object : Any() {
                                [Handler]
                                        fun listen(resp: BosorkResponse) {
                                    when(resp) {
                                        is EchoResponse -> {
                                            echoed = resp.echo
                                            synchronized(lock) {
                                                lock.notifyAll()
                                            }
                                        }
                                        else -> {}
                                    }
                                }
                            })
                        }
                    }
                    else -> {
                    }
                }
                synchronized(lock) {
                    lock.notifyAll()
                }
            }
        }

        app.listen(l)

        synchronized(lock) {
            app.request(LoginRequest(app, SimpleAuthProvider.LOGIN, 1, "guest", "test"))
            lock.wait(10000)
        }

        assertTrue(respReceived)
        assertTrue(loginSuccess)

        synchronized(lock) {
            session.request(EchoRequest(session, EchoServiceProvider.DoubleEcho, 1, echo))
            lock.wait(10000)
        }

        assertEquals(expect, echoed)

    }


}