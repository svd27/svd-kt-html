package ch.passenger.kotlin.basis

import java.util.regex.Pattern
import java.util.regex.Matcher

/**
 * Created by sdju on 24.07.13.
 */
public class URN(val urn:String) :Comparable<URN> {
    private val pSchema = "symblicon|bosork"
    private val pDomConstituent = "[a-z][a-z0=9]*"
    private val pDomain = "$pDomConstituent\\.$pDomConstituent(\\.$pDomConstituent)+"
    private val pEntity = "word|token|interest|application|service|event|test";

    public val scheme : String
    public val thing : String
    public val domain : String
    public val specifier : String


    val pattern = Pattern.compile(
            //"urn:($pSchema):($pEntity):($pDomain):.*",
            //"^urn:($pSchema):($pEntity):($pDomain):([a-z0-9+\\-\\.:+_@;\\$]+)$",
            "^urn:($pSchema):($pEntity):($pDomain):([a-z0-9()+,\\-\\.:=@;\$_!*']+|%[0-9a-f]{2})$",

            Pattern.CASE_INSENSITIVE);
    {
        val m = pattern.matcher(urn)
        if(!m.matches())
            throw IllegalStateException("bad urn $urn")

        scheme = m.group(1)!!
        thing = m.group(2)!!
        domain = m.group(3)!!
        specifier = m.group(5)!!
    }


    public fun dump() {
        val m = pattern.matcher(urn)
        for(i in 0..m.groupCount()) {
            println("g$i: ${m.group(i)}")
        }
    }

    public fun matcher() : Matcher {
        return pattern.matcher(urn)
    }

    public override fun compareTo(other: URN): Int {
        return urn.compareToIgnoreCase(other.urn)
    }

    public fun equals(o: Any?) :Boolean {
        if(o is URN) return compareTo(o) == 0
        return false
    }

    class object {
        private var baseId : Long = 0
        fun word() : String {
            baseId = baseId+1
            return "urn:symblicon:word:root.symblicon.org:${baseId}"
        }

        fun token(id:String) : URN {
            return URN("urn:bosork:token:root.symblicon.org:${id}")
        }

        private var baseInterest : Long = 0
        fun interest(token : String) : String {
            baseInterest = baseInterest+1
            return "urn:bosork:interest:root.symblicon.org:$token:${baseInterest}"
        }

        fun gen(schema:String, entity:String, domain:String, spec:String) : URN {
            return URN("urn:$schema:$entity:$domain:$spec")
        }

        fun service(name:String, domain:String): URN {
            return URN("urn:bosork:service:$domain:$name")
        }

    }
}




