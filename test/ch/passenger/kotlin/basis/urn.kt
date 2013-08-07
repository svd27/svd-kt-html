package ch.passenger.kotlin.basis

import org.junit.Test as test
import org.junit.Ignore as ignore
import kotlin.test.assertEquals


/**
 * Created by sdju on 05.08.13.
 */

class URNTests {
    test fun urnService() {
        val u  = URN.service("login", "test.bosork.org")
        println("${u.scheme} ${u.thing} ${u.domain} ${u.specifier}")
        assertEquals("service", u.thing)
        URN.gen("bosork", "test", "test.bosork.org", "echo")
    }
}