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

/**
 * Created by sdju on 14.08.13.
 */

class EchoRequest(override val session: BosorkSession, override val clientId: Int, val echo : String, val events : Int=0, var wahwah : Int=0) :BosorkRequest {
    override val service : URN = EchoService.ME

    class object {
        {
            val ser = object : AbstractJsonSerialiser() {
                override fun toJson(t: Any, om: ObjectMapper): JsonNode {
                    throw UnsupportedOperationException()
                }


                override fun toObject(node: JsonNode): Any {
                    val rser = Jsonifier.fund(javaClass<BosorkRequest>())
                    val m = rser?.toObject(node)!! as Map<*,*>
                    val session = m["token"] as URN
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

                    val bs = object : BosorkSession {

                        override val app: BosorkApp = BosorkApp(URN.gen("bosork", "application", "test.bosork.org", "test"),
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
                                        throw UnsupportedOperationException()
                                    }
                                    override val id: URN = URN.token("1")
                                }
                            }
                        })
                        override val token: URN = URN.token("1")
                        override val attributes: MutableMap<String, Any?> = HashMap()
                        override val reqBus: MBassador<BosorkRequest> = MBassador(BusConfiguration.Default())
                        override val respBus: MBassador<BosorkResponse> = MBassador(BusConfiguration.Default())
                        override val channels: MutableMap<URN, MBassador<PublishEnvelope>> = HashMap()
                    }
                    return EchoRequest(bs, cid, echo, evs, ww)
                }
            }
            Jsonifier.register(javaClass<EchoRequest>(), ser)
        }
    }

}

class EchoResponse(override val token: URN,override val clientId: Int, val echo : String ) : BosorkResponse {
    override val service: URN = EchoService.ME
    class object {
        {
            val ser = object : AbstractJsonSerialiser() {
                override fun toJson(t: Any, om: ObjectMapper): JsonNode {
                    val rser = Jsonifier.fund(javaClass<BosorkResponse>())
                    val node = rser?.toJson(t, om)!! as ObjectNode
                    val resp = t as EchoResponse
                    node.put("echo", resp.echo)
                }


                override fun toObject(node: JsonNode): Any {
                    throw UnsupportedOperationException()
                }
            }
            Jsonifier.register(javaClass<EchoResponse>(), ser)
        }
    }
}

class EchoEvent

class EchoService() : BosorkService {
    override val id: URN = ME
    override val shortName: String = ME.specifier
    override fun init() {

    }
    override fun destroy() {

    }
    override fun call(req: BosorkRequest): BosorkResponse {
        throw UnsupportedOperationException()
    }class object {
        val ME : URN = URN.service("echo", "test.bosork.org")
    }
}