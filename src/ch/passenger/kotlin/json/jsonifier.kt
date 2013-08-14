package ch.passenger.kotlin.json

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.JsonNode
import java.util.HashMap
import ch.passenger.kotlin.basis.URN
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import ch.passenger.kotlin.basis.ElementEvent
import ch.passenger.kotlin.basis.UpdateEvent

/**
 * Created by sdju on 14.08.13.
 */

trait Jsonifiable {
    fun toJson(om : ObjectMapper) : JsonNode
}

trait JsonSerialiser {
    fun toJson(t:Any, om : ObjectMapper) : JsonNode
}

public object Jsonifier {
    private val registry : MutableMap<Class<*>,JsonSerialiser> = HashMap()
    private val om : ObjectMapper = ObjectMapper();

    {
        registry[javaClass<URN>()] = object : JsonSerialiser {
            override fun toJson(t: Any, om: ObjectMapper): JsonNode {
                if(t is URN) return JsonNodeFactory.instance.textNode(t.urn)!!
                throw IllegalStateException()
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
        }
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
}