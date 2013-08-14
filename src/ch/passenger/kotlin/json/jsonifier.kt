package ch.passenger.kotlin.json

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.JsonNode
import java.util.HashMap
import ch.passenger.kotlin.basis.URN
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import ch.passenger.kotlin.basis.ElementEvent
import ch.passenger.kotlin.basis.UpdateEvent
import java.io.Reader
import com.fasterxml.jackson.databind.node.ObjectNode
import ch.passenger.kotlin.basis.BosorkRequest
import ch.passenger.kotlin.basis.BosorkResponse

/**
 * Created by sdju on 14.08.13.
 */

trait Jsonifiable {
    fun toJson(om : ObjectMapper) : JsonNode
}

trait JsonSerialiser {
    fun toJson(t:Any, om : ObjectMapper) : JsonNode
    fun toObject(node:JsonNode) : Any
}

abstract class AbstractJsonSerialiser : JsonSerialiser {
    protected fun addType(c:Class<*>, node:ObjectNode) {
        node.put("kotlin-type", c.getName())
    }

}

public object Jsonifier {
    private val registry : MutableMap<Class<*>,JsonSerialiser> = HashMap()
    private val om : ObjectMapper = ObjectMapper();

    {
        registry[javaClass<BosorkRequest>()] = object : AbstractJsonSerialiser() {
            override fun toJson(t: Any, om: ObjectMapper): JsonNode {
                val req = t as BosorkRequest
                val node = om.createObjectNode()!!
                addType(javaClass<BosorkRequest>(), node)
                node.put("service", req.service.urn)
                node.put("token", req.session.token.urn)
                node.put("cid", req.clientId)
            }
            override fun toObject(node: JsonNode): Any {
                val service = URN(node.path("service")?.textValue()!!)
                val token = URN(node.path("token")?.textValue()!!)
                val cid = node.path("cid")?.intValue()!!

                return mapOf(Pair("service", service), Pair("token", token), Pair("cid", cid))
            }
        }
        registry[javaClass<BosorkResponse>()] = object : AbstractJsonSerialiser() {
            override fun toJson(t: Any, om: ObjectMapper): JsonNode {
                throw UnsupportedOperationException()
            }
            override fun toObject(node: JsonNode): Any {
                val service = URN(node.path("service")?.textValue()!!)
                val token = URN(node.path("token")?.textValue()!!)
                val cid = node.path("cid")?.intValue()!!

                return mapOf(Pair("service", service), Pair("token", token), Pair("cid", cid))
            }
        }

        registry[javaClass<URN>()] = object : JsonSerialiser {
            override fun toJson(t: Any, om: ObjectMapper): JsonNode {
                if(t is URN) return JsonNodeFactory.instance.textNode(t.urn)!!
                throw IllegalStateException()
            }

            override fun toObject(node: JsonNode): Any {
                return URN(node.textValue()!!)
            }
        }

        registry[javaClass<UpdateEvent<*,*>>()] = object : JsonSerialiser {
            override fun toJson(t: Any, om: ObjectMapper): JsonNode {
                if(t is UpdateEvent<*,*>) {
                    val en = om.createObjectNode()!!
                    en.put("source", t.source.id.urn)
                    en.put("kind", t.kind.name())
                    if(t.old!=null) en.put("old", Jsonifier.serialise(t.old))
                    if(t.new!=null) en.put("new", Jsonifier.serialise(t.new))
                    en.put("property", t.p.name)
                }
                throw IllegalStateException()
            }

            override fun toObject(node: JsonNode): Any {
                throw UnsupportedOperationException("its a one way street (for now)")
            }
        }
    }

    fun deserialise(r:Reader) : Any {
        val jn = om.readTree(r)!!
        val ktype = jn.path("kotlin-type")?.textValue()!!
        val c : Class<*> = Class.forName(ktype)
        if(registry[c]==null) throw IllegalArgumentException("dont know how to handle ${ktype}: ${om.writerWithDefaultPrettyPrinter()?.writeValueAsString(jn)}")
        return registry[c]!!.toObject(jn)
    }

    fun serialise(v:Any) : JsonNode {
        when(v) {
            is Array<*> -> {
                val arr = om.createArrayNode()!!
                for(ae in v) {
                    arr.add(serialise(ae!!))
                }
                return arr
            }
            is JsonNode -> return v
            is Jsonifiable -> return v.toJson(om)
            else -> {
                if(registry[v.javaClass]!=null) {
                    return registry[v.javaClass]!!.toJson(v, om)
                }
                throw IllegalArgumentException("cant handle ${v.javaClass}")
            }
        }
    }

    fun asString(n:JsonNode) :String {
        return om.writerWithDefaultPrettyPrinter()!!.writeValueAsString(n)!!
    }

    public fun register(c:Class<*>, s:JsonSerialiser) {
        registry[c] = s
    }

    public fun fund(c:Class<*>) : JsonSerialiser? {
        return registry[c]
    }
}