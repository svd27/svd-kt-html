package ch.passenger.kotlin.basis

import org.junit.Test as test
import org.junit.Ignore as ignore
import java.util.ArrayList

/**
 * Created with IntelliJ IDEA.
 * User: Duric
 * Date: 04.08.13
 * Time: 05:02
 * To change this template use File | Settings | File Templates.
 */

class EchoRequest(override val session: BosorkSession, override val service: URN,
                  override val clientId: Long, val echo:String) : BosorkRequest
class EchoResponse(override val session: BosorkSession, override val service: URN,
                   override val clientId: Long, val echo:String) : BosorkResponse

class EchoService : BosorkService {
    override val id: URN = EchoServiceProvider.Echo
    override val shortName: String = id.specifier
    override fun init() {

    }
    override fun destroy() {

    }
    override fun call(req: BosorkRequest): BosorkResponse {
        if(req is EchoRequest)
        return EchoResponse(req.session, id, req.clientId, req.echo)
        wrongRequest(req, javaClass<EchoRequest>())
    }

}

class EchoServiceProvider : ServiceProvider {
    override fun creates(): Iterable<URN> {
        return listOf(EchoServiceProvider.Echo)
    }
    override fun create(service: URN): BosorkService {
        return EchoService()
    }
    override fun createOnStartup(): Iterable<URN> {
        return ArrayList<URN>()
    }

    class object {
        val Echo : URN = URN.service("echo", "test.bosork.org")
    }
}

class SimpleAuthProvider

class BosorkAppTests {
    test
    fun simpleLogin() {
        val app = BosorkApp()
    }
}