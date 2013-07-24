/**
 * Created by sdju on 24.07.13.
 */

import org.junit.Test as test
import org.junit.Before as before
import org.junit.After as after
import org.junit.Ignore as ignore
import ch.passenger.kotlin.guice.*
import kotlin.test.assertEquals
import com.google.inject.Inject
import java.lang.annotation.Retention
import java.lang.annotation.Target
import java.lang.annotation.ElementType.*
import com.google.inject.BindingAnnotation
import java.lang.annotation.ElementType.*
import java.lang.annotation.RetentionPolicy.*
import ch.passenger.test.Echo
import ch.passenger.test.Doubled
import com.google.inject.AbstractModule
import com.google.inject.Guice
import kotlin.test.assertNotNull

/*
[BindingAnnotation] [Target(FIELD, PARAMETER, METHOD)] [Retention(RUNTIME)]
annotation class echo

[BindingAnnotation] [Target(FIELD, PARAMETER, METHOD)] [Retention(RUNTIME)]
annotation class double
*/

trait AService {
    fun serve(x:Int) : Int
}

abstract class CAService : AService


Echo
class  AEcho[Inject]() : CAService() {
    override fun serve(x: Int)= x
}

Doubled
class ADouble[Inject]() : CAService() {
    override fun serve(x: Int)= x+x
}

class BasicGuiceTest {
    test
    fun testCreateInjectorWithInstance() {
        fun echo() = injector {
           + module {
               bind<AService>().toInstance(AEcho())
           }
        }

        fun double() = injector {
            + module {
                bind<AService>().toInstance(ADouble())
            }
        }

        assert(echo().getInstance<AService>()!! is AService) {"getInstance did not return AService"}
        assert(echo().getInstance<AService>()!! is AEcho) {"getInstance did not return AEcho"}
        assertEquals(1, echo().getInstance<AService>()!!.serve(1))

        assert(double().getInstance<AService>()!! is AService) {"getInstance did not return AService"}
        assert(double().getInstance<AService>()!! is ADouble) {"getInstance did not return AEcho"}
        assertEquals(2, double().getInstance<AService>()!!.serve(1))
        assertEquals(4, double().getInstance<AService>()!!.serve(2))
    }


    test
    ignore
    fun createInjectorNative() {
        val module = object : AbstractModule() {

            protected override fun configure() {
                bind(javaClass<AService>())!!.to(javaClass<AEcho>())
            }
        }
        val service = Guice.createInjector(module)!!.getInstance(javaClass<AService>())
        assertNotNull(service)
    }
}