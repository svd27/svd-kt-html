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
import ch.passenger.kotlin.basis.BosorkApp
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val log : Logger = LoggerFactory.getLogger(javaClass<Jsonifiable>().getPackage()!!.getName())!!

/**
 * Created by sdju on 14.08.13.
 */

trait Jsonifiable {
    fun toJson(om: ObjectMapper): JsonNode
}

trait JsonSerialiser {
    fun toJson(t: Any, om: ObjectMapper, app: BosorkApp): JsonNode
    fun toObject(node: JsonNode, app: BosorkApp): Any
    fun components(node: JsonNode, app: BosorkApp): Map<String, Any>
}

abstract class AbstractJsonSerialiser : JsonSerialiser {
    override fun toJson(t: Any, om: ObjectMapper, app: BosorkApp): JsonNode {
        throw UnsupportedOperationException()
    }
    override fun toObject(node: JsonNode, app: BosorkApp): Any {
        throw UnsupportedOperationException()
    }
    override fun components(node: JsonNode, app: BosorkApp): Map<String, Any> {
        return HashMap()
    }
}

abstract class BosorkRequestJson : AbstractJsonSerialiser() {
    override fun toJson(t: Any, om: ObjectMapper, app: BosorkApp): JsonNode {
        throw UnsupportedOperationException("its a one way street (for now)")
    }

    override fun components(node: JsonNode, app: BosorkApp): Map<String, Any> {
        val service = URN(node.path("service")?.textValue()!!)
        val token = URN(node.path("token")?.textValue()!!)
        val cid = node.path("cid")?.intValue()!!

        return mapOf(Pair("service", service), Pair("token", token), Pair("cid", cid))
    }
}

abstract class BosorkResponseJson : AbstractJsonSerialiser() {
    override fun toJson(t: Any, om: ObjectMapper, app: BosorkApp): JsonNode {
        val on = om.createObjectNode()!!
        val r = t as BosorkResponse
        on.put("token", r.token.urn)
        on.put("service", r.service.urn)
        on.put("cid", r.clientId)
        return on
    }
    override fun toObject(node: JsonNode, app: BosorkApp): Any {
        throw UnsupportedOperationException("its a one way street (for now)")
    }
}

abstract class ElementEventJson : AbstractJsonSerialiser() {
    override fun toJson(t: Any, om: ObjectMapper, app: BosorkApp): JsonNode {
        val en = om.createObjectNode()!!
        if(t is ElementEvent<*>) {
            en.put("source", t.source.id.urn)
            en.put("kind", t.kind.name())
            en.put("element", Jsonifier.serialise(t.source, app))
            return en
        }
        throw IllegalArgumentException("cant handle $t")
    }
}

public object Jsonifier {
    private val registry: MutableMap<Class<*>, JsonSerialiser> = HashMap()
    private val om: ObjectMapper = ObjectMapper();

    {
        registry[javaClass<URN>()] = object : JsonSerialiser {
            override fun toJson(t: Any, om: ObjectMapper, app: BosorkApp): JsonNode {
                if(t is URN) return JsonNodeFactory.instance.textNode(t.urn)!!
                throw IllegalStateException()
            }

            override fun toObject(node: JsonNode, app: BosorkApp): Any {
                return URN(node.textValue()!!)
            }


            override fun components(node: JsonNode, app: BosorkApp): Map<String, Any> {
                throw UnsupportedOperationException()
            }
        }

        registry[javaClass<UpdateEvent<*, *>>()] = object : JsonSerialiser {
            override fun toJson(t: Any, om: ObjectMapper, app: BosorkApp): JsonNode {
                if(t is UpdateEvent<*, *>) {
                    val en = om.createObjectNode()!!
                    en.put("source", t.source.id.urn)
                    en.put("kind", t.kind.name())
                    if(t.old != null) en.put("old", Jsonifier.serialise(t.old, app))
                    if(t.new != null) en.put("new", Jsonifier.serialise(t.new, app))
                    en.put("property", t.p.name)
                    return en
                }
                throw IllegalStateException()
            }

            override fun toObject(node: JsonNode, app: BosorkApp): Any {
                throw UnsupportedOperationException("its a one way street (for now)")
            }


            override fun components(node: JsonNode, app: BosorkApp): Map<String, Any> {
                throw UnsupportedOperationException()
            }
        }
    }

    fun deserialise(r: Reader, app: BosorkApp, target:Class<*>): Any {
        val jn = om.readTree(r)!!
        //val ktype = jn.path("kotlin-type")?.textValue()!!
        //val c: Class<*> = Class.forName(ktype)
        val c = target
        println("registry: $registry size: ${registry.size()}")
        val jsr = registry.get(c)
        if(jsr == null) throw IllegalArgumentException("dont know how to handle ${c}: ${om.writerWithDefaultPrettyPrinter()?.writeValueAsString(jn)}")
        return registry[c]!!.toObject(jn, app)
    }

    fun serialise(v: Any, app: BosorkApp): JsonNode {
        when(v) {
            is Array<*> -> {
                val arr = om.createArrayNode()!!
                for(ae in v) {
                    arr.add(serialise(ae!!, app))
                }
                return arr
            }
            is JsonNode -> return v
            is Jsonifiable -> return v.toJson(om)
            else -> {
                if(registry[v.javaClass] != null) {
                    return registry[v.javaClass]!!.toJson(v, om, app)
                }
                throw IllegalArgumentException("cant handle ${v.javaClass}")
            }
        }
    }

    fun asString(n: JsonNode): String {
        return om.writerWithDefaultPrettyPrinter()!!.writeValueAsString(n)!!
    }

    public fun register(c: Class<*>, s: JsonSerialiser) {
        log.info("JSON registry: $c -> $s")
        registry[c] = s
    }

    public fun find(c: Class<*>): JsonSerialiser? {
        return registry[c]
    }
}