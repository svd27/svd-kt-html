package ch.passenger.kotlin.jetty.support

import ch.passenger.kotlin.basis.BosorkService
import ch.passenger.kotlin.basis.URN
import ch.passenger.kotlin.basis.BosorkRequest
import ch.passenger.kotlin.basis.BosorkResponse
import ch.passenger.kotlin.basis.BosorkSession
import ch.passenger.kotlin.json.Jsonifier
import ch.passenger.kotlin.json.JsonSerialiser
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.JsonNode
import ch.passenger.kotlin.json.AbstractJsonSerialiser
import com.fasterxml.jackson.databind.node.ObjectNode
import ch.passenger.kotlin.basis.BosorkApp
import net.engio.mbassy.bus.MBassador
import ch.passenger.kotlin.basis.PublishEnvelope
import java.util.ArrayList
import ch.passenger.kotlin.basis.ServiceProvider
import ch.passenger.kotlin.basis.SessionFactoryProvider
import ch.passenger.kotlin.basis.SessionFactory
import ch.passenger.kotlin.basis.AuthProvider
import ch.passenger.kotlin.basis.AuthService
import java.util.HashMap
import net.engio.mbassy.bus.BusConfiguration
import ch.passenger.kotlin.basis.LoginRequest
import ch.passenger.kotlin.basis.LoginResponse
import ch.passenger.kotlin.json.BosorkRequestJson
import ch.passenger.kotlin.json.BosorkResponseJson
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import ch.passenger.kotlin.basis.EventTypes
import ch.passenger.kotlin.basis.ElementEvent
import ch.passenger.kotlin.basis.Identifiable
import ch.passenger.kotlin.json.Jsonifiable
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import java.util.concurrent.TimeUnit
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ch.passenger.kotlin.json.ElementEventJson

/**
 * Created by sdju on 14.08.13.
 */

val log : Logger = LoggerFactory.getLogger(javaClass<EchoRequest>().getPackage()!!.getName())!!

val DUMMY_APP : BosorkApp = BosorkApp(URN.gen("bosork", "application", "test.bosork.org", "test"),
        ArrayList<ServiceProvider>(), object : SessionFactoryProvider {
    override fun provider(app: BosorkApp): SessionFactory {
        return object : SessionFactory {
            override fun createSession(req: LoginRequest, token: URN): BosorkSession {
                throw UnsupportedOperationException()
            }
        }
    }
}, object : AuthProvider {
    override fun createAuth(app: BosorkApp, sp: SessionFactory): AuthService {
        return object : AuthService {

            protected override val sp: SessionFactory = sp
            override fun login(req: LoginRequest): LoginResponse {
                return LoginResponse(DUMMY_SESSION, 1, null)
            }
            override val id: URN = URN.token("1")
        }
    }
})

val DUMMY_SESSION = object : BosorkSession {
    override val app: BosorkApp = DUMMY_APP
    override val token: URN = URN.token("1")
    override val attributes: MutableMap<String, Any?> = HashMap()
    override val reqBus: MBassador<BosorkRequest> = MBassador(BusConfiguration.Default())
    override val respBus: MBassador<BosorkResponse> = MBassador(BusConfiguration.Default())
    override val channels: MutableMap<URN, MBassador<PublishEnvelope>> = HashMap()
}



class EchoRequest(override val session: BosorkSession, override val clientId: Int, val echo : String, val events : Int=0, var wahwah : Int=0) :BosorkRequest {
    override val service : URN = EchoService.ME

    class object {
        val ser = object : BosorkRequestJson() {
            override fun toJson(t: Any, om: ObjectMapper, app:BosorkApp): JsonNode {
                throw UnsupportedOperationException()
            }


            override fun toObject(node: JsonNode, app:BosorkApp): Any {
                val m = components(node, app)
                val token = m["token"] as URN
                val service = m["service"] as URN
                val cid = m["cid"] as Int

                val echo = node.path("echo")!!.textValue()!!

                var ww = 0
                if(node.has("wahwah")) {
                    ww = node.path("wahwah")?.intValue()!!
                }
                var evs = 0
                if(node.has("events")) {
                    evs = node.path("events")?.intValue()!!
                }
                val session = app.session(token)

                if(session==null) throw IllegalStateException()

                return EchoRequest(session, cid, echo, evs, ww)
            }
        }

        fun init() {
            Jsonifier.register(javaClass<EchoRequest>(), ser)
        }

        {
            //TODO: some stupid Kotlin bug seems not to call this
            Jsonifier.register(javaClass<EchoRequest>(), ser)
        }

    }

}

class EchoResponse(override val token: URN,override val clientId: Int, val echo : String ) : BosorkResponse {
    override val service: URN = EchoService.ME
    class object {
        {
            val ser = object : BosorkResponseJson() {
                override fun toJson(t: Any, om: ObjectMapper, app:BosorkApp): JsonNode {
                    val node = super.toJson(t, om, app) as ObjectNode

                    val resp = t as EchoResponse
                    node.put("echo", resp.echo)
                }


                override fun toObject(node: JsonNode, app:BosorkApp): Any {
                    throw UnsupportedOperationException()
                }
            }
            Jsonifier.register(javaClass<EchoResponse>(), ser)
            fun init() : Unit {
                Jsonifier.register(javaClass<EchoResponse>(), ser)
            }
        }
    }
}


class EchoHolder(val echo:String, val repeat:Int) : Identifiable, Jsonifiable {
    override val id :URN = URN.gen(EchoService.ME.scheme, "event", EchoService.ME.domain, "echo")

    override fun toJson(om: ObjectMapper): JsonNode {
        return JsonNodeFactory.instance.textNode(echo+":$repeat")!!
    }
}
class EchoEvent(echo:String, repeat:Int) : ElementEvent<EchoHolder>(EchoHolder(echo, repeat), EventTypes.ADD) {
    class object {
        val json = object : ElementEventJson() {

            override fun toJson(t: Any, om: ObjectMapper, app: BosorkApp): JsonNode {
                val on =  super<ElementEventJson>.toJson(t, om, app) as ObjectNode
                val ev = t as EchoEvent
                val eh = ev.source
                on.put("echo", Jsonifier.serialise(eh, app))
                return on
            }
        }
        {
            Jsonifier.register(javaClass<EchoEvent>(), json)
        }
    }
}

fun Int.doTimes(cb: ()->Unit) {
    for(i in 0..this) cb()
}

class EchoService(val app : BosorkApp) : BosorkService {
    override val id: URN = ME
    override val shortName: String = ME.specifier
    override fun init() {
        //stupid really
        EchoRequest.init()
    }
    override fun destroy() {

    }
    override fun call(req: BosorkRequest): BosorkResponse {
        val er = req as EchoRequest
        val echo = er.echo
        var result = StringBuilder()
        if(er.wahwah>0) {
            echo.forEach {
                current ->
                er.wahwah.doTimes {
                    result.append(current)
                }
            }
        } else result.append(echo)

        if(er.events>0) {
            val scheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
            var scheduledFuture : ScheduledFuture<*>? = null
            val channel = req.session.requestChannel(req.service)
            val r : Runnable = object : Runnable {
                var lastEcho = echo
                val events = er.events
                var invoked = 0
                public override fun run() {
                    try {
                        if(invoked>=events) return
                        val sb = StringBuilder()
                        lastEcho.forEach {
                            sb.append("$it$it")
                        }
                        lastEcho = sb.toString()
                        invoked++
                        channel.publishAsync(PublishEnvelope(id, null, EchoEvent(lastEcho, invoked)))
                        log.info("$invoked time invoked: $lastEcho")
                        if(invoked>=events) {
                            log.info("had enough")
                            scheduledFuture?.cancel(true)
                        }
                    } catch(e: Throwable) {
                        e.printStackTrace()
                    }
                }
            }

            scheduledFuture = scheduledExecutorService.scheduleAtFixedRate(r, 1, 1, TimeUnit.SECONDS)
        }


        return EchoResponse(req.session.token, req.clientId, result.toString())
    }


    override fun request(): Class<out Any?> = javaClass<EchoRequest>()
    override fun response(): Class<out Any?> = javaClass<EchoResponse>()

    class object {
        val ME : URN = URN.service("echo", "test.bosork.org")
    }
}

class EchoServiceProvider : ServiceProvider {
    override fun creates(): Iterable<URN> {
        return listOf(EchoService.ME)
    }
    override fun create(service: URN, app: BosorkApp): BosorkService {
        return EchoService(app)
    }
    override fun createOnStartup(): Iterable<URN> {
        return creates()
    }
}