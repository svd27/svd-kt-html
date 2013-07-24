package ch.passenger.kotlin.basis

import java.util.HashSet
import java.util.ArrayList
import java.util.HashMap

/**
 * Created by sdju on 24.07.13.
 */

class A(private val ai : Int) : Identifiable {
    override val id: URN = URN.gen("bosork", "thing", "test.bosork.org", "$ai")
}

class AProducer : ElementProducer<A> {

    override fun produce(f: Filter<A>): Iterator<A> {
        throw UnsupportedOperationException()
    }
    override fun retrieve(vararg id: Long): Iterable<A> {
        throw UnsupportedOperationException()
    }
    protected override val observers: MutableSet<Observer<A>> = HashSet()
}

class AInterest(override val id:URN, override val producer : AProducer, override val name: String ) : Interest<A> {
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