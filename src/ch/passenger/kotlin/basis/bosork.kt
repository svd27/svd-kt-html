package ch.passenger.kotlin.basis

import java.util.HashMap
import java.util.ArrayList
import java.util.WeakHashMap
import net.engio.mbassy.bus.MBassador
import net.engio.mbassy.bus.BusConfiguration
import net.engio.mbassy.listener.Handler

/**
 * Created with IntelliJ IDEA.
 * User: Duric
 * Date: 03.08.13
 * Time: 21:20
 * To change this template use File | Settings | File Templates.
 */
open class BosorkError(val message:String, val cause:Throwable?) : Exception(message, cause)
class BosorkServiceNotFound(val service:URN) : BosorkError("service ${service.urn} not found", null)
class BosorkLoginFailed(val user:String) : BosorkError("login for user $user failed", null)

class WrongRequestType(expected:Class<*>, received:Class<*>) :
BosorkError("wrong request type ${received.getName()} expected: ${expected.getName()}",null)

trait BosorkSession {
    val app : BosorkApp
    val token: URN
    val attributes: MutableMap<String, Any?>
    val reqBus : MBassador<BosorkRequest>
    val respBus : MBassador<BosorkResponse>

    fun destroy()

    Handler
    public final fun request(req: BosorkRequest) {
        try {
            val s = app.service(req.service)
            if(s!=null) respBus.post(s.call(req))
            else respBus.post(BosorkErrorResponse.make(req, BosorkServiceNotFound()))
        } catch(e: Exception) {
            respBus.post(BosorkErrorResponse.make(req, e))
        }
    }
}

class SimpleSession(override val token: URN, override val app : BosorkApp): BosorkSession {
    override val attributes: MutableMap<String, Any?> = HashMap()
    override val reqBus : MBassador<BosorkRequest> = MBassador(BusConfiguration.Default())
    override val respBus : MBassador<BosorkResponse> = MBassador(BusConfiguration.Default())

    override fun destroy() {
        attributes.clear()
        reqBus.shutdown()
        respBus.shutdown()
    }
}

class NullSession(override val app : BosorkApp) : BosorkSession {
    override val token: URN = URN("")
    override val attributes: MutableMap<String, Any?> = HashMap(0)
    override val reqBus : MBassador<BosorkRequest> = MBassador(BusConfiguration.Default())
    override val respBus : MBassador<BosorkResponse> = MBassador(BusConfiguration.Default())

    override fun destroy() {
        attributes.clear()
        reqBus.shutdown()
        respBus.shutdown()
    }
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


trait BosorkService : Identifiable {
    val shortName: String
    fun init()
    fun destroy()
    fun call(req:BosorkRequest) : BosorkResponse
    fun wrongRequest(req:BosorkRequest, expected:Class<*>) :BosorkResponse {
        throw WrongRequestType(expected, req.javaClass)
    }
}

abstract class AbstractService(override public val id: URN, override public val shortName: String): BosorkService

trait ServiceProvider {
    fun creates(): Iterable<URN>
    fun create(service:URN): BosorkService
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

    fun start() {

        providers.forEach {
            p ->
           p.createOnStartup().forEach {
               services[it] = p.create(it)
           }
       }
    }

    fun stop() {

    }

    fun service(urn:URN) : BosorkService? {
        return services[urn]
    }
}

