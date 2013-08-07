package ch.passenger.kotlin.basis

import java.util.HashMap
import java.util.ArrayList
import java.util.WeakHashMap
import net.engio.mbassy.bus.MBassador
import net.engio.mbassy.bus.BusConfiguration
import net.engio.mbassy.listener.Handler
import org.slf4j.LoggerFactory

/**
 * Created with IntelliJ IDEA.
 * User: Duric
 * Date: 03.08.13
 * Time: 21:20
 * To change this template use File | Settings | File Templates.
 */
private val log = LoggerFactory.getLogger(javaClass<BosorkApp>()?.getPackage()?.getName())!!

open class BosorkError(val message:String, val cause:Throwable?=null) : Exception(message, cause)
class BosorkServiceNotFound(val service:URN) : BosorkError("service ${service.urn} not found", null)
class BosorkWrongService(val exptected:URN, val actual:URN) : BosorkError("service ${exptected.urn} cant handle: ${actual.urn}", null)
class BosorkLoginFailed(val user:String) : BosorkError("login for user $user failed", null)

class WrongRequestType(expected:Class<*>, received:Class<*>) :
BosorkError("wrong request type ${received.getName()} expected: ${expected.getName()}",null)

class PublishEnvelope(val source : URN, val destination : URN?, val event : Event<*>)

trait BosorkSession {
    val app : BosorkApp
    val token: URN
    val attributes: MutableMap<String, Any?>
    val reqBus : MBassador<BosorkRequest>
    val respBus : MBassador<BosorkResponse>
    val channels : MutableMap<URN,MBassador<PublishEnvelope>>


    fun start() {
        reqBus.subscribe(this)
    }

    fun destroy() {
        attributes.clear()
        reqBus.shutdown()
        respBus.shutdown()
        channels.values().forEach {
            it.shutdown()
        }
        channels.clear()
    }

    Handler
    public final fun request(req: BosorkRequest) {
        log.info("handling ${req.service.urn}: ${req.session.token.urn} ${req.clientId} class: ${req.javaClass}")
        try {
            val s = app.service(req.service)
            log.info("found service ${s?.id?.urn}")
            if(s!=null) respBus.publishAsync(s.invoke(req))
            else respBus.publishAsync(BosorkErrorResponse.make(req, BosorkServiceNotFound(req.service)))
        } catch(e: Exception) {
            log.error("${e.getMessage()}", e)
            respBus.publishAsync(BosorkErrorResponse.make(req, e))
        }
    }

    public fun listen(l:Any) {
        respBus.subscribe(l)
    }

    fun requestChannel(owner:URN) : MBassador<PublishEnvelope> {
        if(!channels.containsKey(owner)) channels[owner] = MBassador(BusConfiguration.Default())
        log.info("retrieve channel ${owner.urn} -> ${channels[owner]}")
        return channels[owner]!!
    }

    fun subscribe(channel:URN, l:Any) {
        requestChannel(channel).subscribe(l)
    }
}

class AbstractSession(override val token: URN, override val app : BosorkApp): BosorkSession {
    override val attributes: MutableMap<String, Any?> = HashMap()
    override val reqBus : MBassador<BosorkRequest> = MBassador(BusConfiguration.Default())
    override val respBus : MBassador<BosorkResponse> = MBassador(BusConfiguration.Default())
    override val channels: MutableMap<URN, MBassador<PublishEnvelope>> = HashMap()
}

class NullSession(override val app : BosorkApp) : BosorkSession {
    override val token: URN = URN.token("null")
    override val attributes: MutableMap<String, Any?> = HashMap(0)
    override val reqBus : MBassador<BosorkRequest> = MBassador(BusConfiguration.Default())
    override val respBus : MBassador<BosorkResponse> = MBassador(BusConfiguration.Default())
    override val channels: MutableMap<URN, MBassador<PublishEnvelope>> = HashMap()
}

trait BosorkRequest {
    val session : BosorkSession
    val service : URN
    val clientId : Long
}

trait BosorkResponse {
    val session : BosorkSession
    val service : URN
    val clientId : Long
}

public open class BosorkErrorResponse(override val session: BosorkSession,
                                      override val service:URN, override val clientId: Long,
                                      val cause:Throwable)
: BosorkResponse {
    class object {
        fun make(req:BosorkRequest, t:Throwable) : BosorkErrorResponse {
            return BosorkErrorResponse(req.session, req.service, req.clientId, t)
        }
    }
}

trait Publisher {
    fun publish(destination:BosorkSession?, event:Event<*>)
}

val NOOPPublisher = object : Publisher {
    override fun publish(destination:BosorkSession?, event:Event<*>) {

    }
}

trait BosorkService : Identifiable {
    val shortName: String
    fun init()
    fun destroy()
    fun call(req:BosorkRequest) : BosorkResponse
    fun invoke(req:BosorkRequest) : BosorkResponse {
        if(req.service.urn!=id.urn)
            throw BosorkWrongService(id, req.service)
        log.info("calling: ${id.urn}")
        return call(req)
    }
    fun wrongRequest(req:BosorkRequest, expected:Class<*>) :BosorkResponse {
        throw WrongRequestType(expected, req.javaClass)
    }
}



abstract class AbstractService(override public val id: URN, override public val shortName: String): BosorkService {
    override fun wrongRequest(req:BosorkRequest, expected:Class<*>) :BosorkResponse {
        throw WrongRequestType(expected, req.javaClass)
    }

    override fun invoke(req:BosorkRequest) : BosorkResponse {
        if(req.service.urn!=id.urn)
            throw BosorkWrongService(id, req.service)
        log.info("calling: ${id.urn}")
        return call(req)
    }
}

trait ServiceProvider {
    fun creates(): Iterable<URN>
    fun create(service:URN, app:BosorkApp): BosorkService
    fun createOnStartup() : Iterable<URN>
}


class LoginRequest(val app : BosorkApp, override val service:URN, override val clientId:Long,
                   val user:String, val pwd:String) : BosorkRequest {
    override val session: BosorkSession = NullSession(app)
}

class LoginResponse(override val session:BosorkSession,override val service:URN,
                    override val clientId:Long, val error:Throwable?) : BosorkResponse

trait Authoriser: BosorkService



class LogoutRequest(override val session:BosorkSession, override val service:URN,
                    override val clientId:Long) : BosorkRequest

class LogoutResponse(override val session:BosorkSession,override val service:URN,
                    override val clientId:Long) : BosorkResponse


trait Finisher : BosorkService

trait AuthProvider : ServiceProvider {
    val login : URN
    val finisher : URN


    override fun creates(): Iterable<URN> {
        return listOf(login,finisher)
    }

    override fun createOnStartup(): Iterable<URN> {
        return creates()
    }
}

public class BosorkApp(val providers: Iterable<ServiceProvider>) {
    private val sessions : MutableMap<URN,BosorkSession> = HashMap()
    private val services : MutableMap<URN,BosorkService> = HashMap()
    val reqBus : MBassador<BosorkRequest> = MBassador(BusConfiguration.Default())
    val respBus : MBassador<BosorkResponse> = MBassador(BusConfiguration.Default())

    fun start() {

        providers.forEach {
            p ->
            log.info("provider ${p}")
           p.createOnStartup().forEach {
               log.info("service ${it.urn}")
               services[it] = p.create(it, this)
           }
       }
       reqBus.subscribe(this)
    }

    [Handler]
    fun handle(req:BosorkRequest) {
        log.debug("handling ${req.service}: ${req.session} ${req.clientId} class: ${req.javaClass}")
        val s = service(req.service)
        if(s==null) throw BosorkServiceNotFound(req.service)
        respBus.publishAsync(s.invoke(req))
    }

    fun stop() {

    }

    fun service(urn:URN) : BosorkService? {
        return services[urn]
    }

    fun request(req:BosorkRequest) {
        reqBus.publishAsync(req)
    }

    fun listen(l:Any) {
        respBus.subscribe(l)
    }

}

