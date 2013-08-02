package ch.passenger.kotlin.basis

import java.util.HashSet
import java.util.ArrayList
import java.util.HashMap
import com.google.inject.name.Named as named
import com.google.inject.Inject as inject
import org.junit.Test as test
import ch.passenger.kotlin.guice.*
import com.google.inject.Guice
import com.google.inject.TypeLiteral


/**
 * Created by sdju on 24.07.13.
 */

class A(private val ai : Int) : Identifiable {
    override val id: URN = URN.gen("bosork", "thing", "test.bosork.org", "$ai")
}

named("test-aproducer")
trait AProducer : ElementProducer<A> {
    override fun produce(f: Filter<A>): Iterator<A>
    override fun retrieve(vararg id: Long): Iterable<A>

}

class TestAProducer [inject]() : AProducer {
    protected override val observers: MutableSet<Observer<A>> = HashSet()


    override fun produce(f: Filter<A>): Iterator<A> {
        throw UnsupportedOperationException()
    }
    override fun retrieve(vararg id: Long): Iterable<A> {
        throw UnsupportedOperationException()
    }
}



class AInterest [inject] (override val id:URN, override val producer : AProducer, override val name: String ) : Interest<A> {
    public override var rowsPerPage: Int = 10
    public override var current: Int = 0
    public override var page: Page<A> = Page<A>(ArrayList(), 0, 0, 0, 0, 0, 0)
    override var filter: Filter<A> = IdentityFilter<A>()
    private val mapped : MutableMap<URN,A> = HashMap()
    override fun addHook(e: A): A? {
        if(mapped[e.id]!=null) return mapped[e.id]
        mapped[e.id] = e
        return e
    }
    override fun removeHook(e: A): A? {
        return mapped.remove(e.id)
    }

    public override val elements: MutableList<A> get() = ArrayList(mapped.values())


    protected override val observers: MutableSet<Observer<A>> = HashSet()

}

class InterestTests() {
    test
    fun testModule() {
        println(javaClass<ElementProducer<A>>())
        val gin = injector {
            +module {
                bind<ElementProducer<A>>().to<AProducer>()
                bind<AProducer>().to<TestAProducer>()
            }
        }

        val allBindings = gin.getAllBindings()
        println(gin.getAllBindings())
        val ep = gin.getInstance<ElementProducer<A>>()
        assert(ep is AProducer)
    }
}

