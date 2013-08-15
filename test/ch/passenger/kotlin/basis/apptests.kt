package ch.passenger.kotlin.basis

import org.junit.Test as test
import org.junit.Ignore as ignore
import java.util.ArrayList
import net.engio.mbassy.listener.Handler
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.ScheduledFuture
import net.engio.mbassy.listener.Invoke
import kotlin.test.assertNotNull

/**
 * Created with IntelliJ IDEA.
 * User: Duric
 * Date: 04.08.13
 * Time: 05:02
 * To change this template use File | Settings | File Templates.
 */

private val testLog = LoggerFactory.getLogger(javaClass<EchoRequest>()?.getPackage()?.getName())!!


class EchoRequest(override val session: BosorkSession, override val service: URN,
                  override val clientId: Int, val echo: String) : BosorkRequest {
    {

    }
}
class EchoResponse(override val token: URN, override val service: URN,
                   override val clientId: Int, val echo: String) : BosorkResponse {
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
            return EchoResponse(req.session.token, id, req.clientId, req.echo)
        return wrongRequest(req, javaClass<EchoRequest>())
    }


    override fun request(): Class<out Any?> {
        return javaClass<EchoRequest>()
    }
    override fun response(): Class<out Any?> {
        return javaClass<EchoResponse>()
    }
}

class TheEcho(val echo : String, override val id:URN) : Identifiable

class EchoEvent(val echo:TheEcho) : ElementEvent<TheEcho>(echo, EventTypes.ADD)

class DoubleEchoService : BosorkService {
    override val id: URN = EchoServiceProvider.DoubleEcho
    override val shortName: String = id.specifier
    var lastEcho = "";
    override fun init() {

    }
    override fun destroy() {

    }
    override fun call(req: BosorkRequest): BosorkResponse {
        if(req is EchoRequest){
            val sb = StringBuilder()
            req.echo.forEach { sb.append("$it$it") }
            lastEcho = sb.toString()
            val channel = req.session.requestChannel(req.service)
            val scheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
            var scheduledFuture : ScheduledFuture<*>? = null
            val r : Runnable = object : Runnable {
                public override fun run() {
                    try {
                        testLog.info("!!!!!!!CRT EVENT!!!!!!!")
                        val sb = StringBuilder()
                        lastEcho.forEach {
                            sb.append("$it$it")
                        }
                        lastEcho = sb.toString()
                        testLog.info("now $lastEcho: publishing on ${channel}")
                        channel.publishAsync(PublishEnvelope(id, null, EchoEvent(TheEcho(lastEcho,
                                URN.gen("bosork", "test", "test.bosork.org", "echo")))))
                        if(lastEcho.length()>30) {
                            testLog.info("$lastEcho getting too long... Bye")
                            scheduledFuture?.cancel(true)
                        }
                    } catch(e: Throwable) {
                        testLog.error(e.getMessage(), e)
                    }
                }
            }

            scheduledFuture = scheduledExecutorService.scheduleAtFixedRate(r, 1, 1, TimeUnit.SECONDS)

            return EchoResponse(req.session.token, id, req.clientId, sb.toString())
        }
        return wrongRequest(req, javaClass<EchoRequest>())
    }

    override fun request(): Class<out Any?> {
        return javaClass<EchoRequest>()
    }
    override fun response(): Class<out Any?> {
        return javaClass<EchoResponse>()
    }
}

class EchoServiceProvider : ServiceProvider {
    override fun creates(): Iterable<URN> {
        return listOf(EchoServiceProvider.Echo, EchoServiceProvider.DoubleEcho)
    }
    override fun create(service: URN, app: BosorkApp): BosorkService {
        var s : BosorkService? = null
        if(service.urn==EchoServiceProvider.Echo.urn) s = EchoService()
        if(service.urn==EchoServiceProvider.DoubleEcho.urn) s = DoubleEchoService()
        testLog.info("created service ${s?.id?.urn} for ${service.urn}")
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

    override fun createAuth(app:BosorkApp, sp: SessionFactory): AuthService {
        return object : AuthService {
            protected override val sp: SessionFactory = sp
            override fun login(req: LoginRequest): LoginResponse {
                if(req.user == "guest" && req.pwd == "test") {
                    val s = sp.createSession(req, URN.token("test"))
                    return LoginResponse(s, req.clientId)
                }

                return LoginResponse(NullSession(app), req.clientId, BosorkLoginFailed(req.user))
            }

            val shortName: String = LOGIN.specifier

            override val id: URN = LOGIN
        }
    }

    class object {
        val LOGIN = URN.service("login", "test.bosork.org")
        val LOGOUT = URN.service("logout", "test.bosork.org")
    }
}

class TestSessionFactoryProvider : SessionFactoryProvider {

    override fun provider(app: BosorkApp): SessionFactory {
        return TestSessionProvider(app)
    }
}

class TestSessionProvider(private val app : BosorkApp) : SessionFactory {
    override fun createSession(req:LoginRequest, token: URN): BosorkSession {
        return AbstractSession(token, app)
    }
}

class BosorkAppTests {
    test
    fun simpleLogin() {
        val app = BosorkApp(URN.gen("bosork", "application", "test.bosork.org", "echo-app"), listOf(EchoServiceProvider()), TestSessionFactoryProvider(), SimpleAuthProvider())
        app.start()

        val loginResponse = app.login(LoginRequest(1, "guest", "test"))

        assertNotNull(loginResponse.session)
        assertNotNull(loginResponse.session.token)
    }

    test
    fun simpleEcho() {
        val sap = SimpleAuthProvider()
        val app = BosorkApp(URN.gen("bosork", "application", "test.bosork.org", "echo-app"), listOf(EchoServiceProvider()), TestSessionFactoryProvider(), SimpleAuthProvider())
        app.start()

        val loginResponse = app.login(LoginRequest(1, "guest", "test"))
        var session : BosorkSession = loginResponse.session
        val echo = "A"
        var echoed = ""
        val lock = Object()
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


        synchronized(lock) {
            session.request(EchoRequest(session, EchoServiceProvider.Echo, 1, echo))
            lock.wait(1000)
        }

        assertEquals(echo, echoed)

    }

    test
    fun doubleEcho() {
        val sap = SimpleAuthProvider()
        val app = BosorkApp(URN.gen("bosork", "application", "test.bosork.org", "echo-app"), listOf(EchoServiceProvider()), TestSessionFactoryProvider(), SimpleAuthProvider())
        app.start()
        val loginResponse = app.login(LoginRequest(1, "guest", "test"))
        var session : BosorkSession = loginResponse.session
        val echo = "ABC"
        val expect = "AABBCC"
        var echoed = ""
        val lock = Object()

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

        synchronized(lock) {
            session.request(EchoRequest(session, EchoServiceProvider.DoubleEcho, 1, echo))
            lock.wait(10000)
        }

        assertEquals(expect, echoed)

    }

    test
    fun doubleEchoEvent() {
        val sap = SimpleAuthProvider()
        val app = BosorkApp(URN.gen("bosork", "application", "test.bosork.org", "echo-app"), listOf(EchoServiceProvider()), TestSessionFactoryProvider(), SimpleAuthProvider())
        app.start()
        val loginResponse = app.login(LoginRequest(1, "guest", "test"))
        var session : BosorkSession = loginResponse.session
        val echo = "ABC"
        val expect = "AABBCC"
        var echoed = ""
        val lock = Object()
        val elock = Object()

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

        var eventReceived :Boolean = false

        val el = object : Any() {
            [Handler(delivery = Invoke.Asynchronously)]
            fun handle(env:PublishEnvelope) {
                testLog.info("handle: ${env.event} from ${env.source} to ${env.destination}")
                val event = env.event as EchoEvent

                testLog.info("${event.source.echo}")
                eventReceived = true
                synchronized(elock) {
                    elock.notifyAll()
                }
            }
        }

        synchronized(lock) {
            session.subscribe(EchoServiceProvider.DoubleEcho, el)
            session.request(EchoRequest(session, EchoServiceProvider.DoubleEcho, 1, echo))
        }

        synchronized(elock) {
            if(!eventReceived) elock.wait(10000)
        }

        assertEquals(expect, echoed)
        assertTrue(eventReceived)

    }


}