package ch.passenger.kotlin.jetty.support

import org.junit.Test as test
import ch.passenger.kotlin.basis.URN
import ch.passenger.kotlin.basis.LoginRequest
import com.fasterxml.jackson.databind.ObjectMapper
import ch.passenger.kotlin.json.Jsonifier
import java.io.StringReader
import kotlin.test.assertEquals

/**
 * Created by sdju on 14.08.13.
 */
class JsonTests {
    [test]
    fun testReq() {
        val token = URN.token("1")
        val om = ObjectMapper()

        val on = om.createObjectNode()!!
        on.put("token", token.urn)
        on.put("service", EchoService.ME.urn)
        on.put("cid", 1.toInt())
        on.put("echo", "test")
        on.put("wahwah", 2.toInt())
        val s = om.writerWithDefaultPrettyPrinter()?.writeValueAsString(on)!!

        DUMMY_APP.login(LoginRequest(1, "", ""))
        val obj = Jsonifier.deserialise(StringReader(s), DUMMY_APP, javaClass<EchoRequest>())
        assert(obj.javaClass.equals(javaClass<EchoRequest>()))
        val req = obj as EchoRequest
        assertEquals("test", req.echo)
        assertEquals(2, req.wahwah)
    }
}