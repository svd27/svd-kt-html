package ch.passenger.kotlin.basis

import java.util.HashMap

/**
 * Created by sdju on 17.07.13.
 */
public object InterestManager {
    fun createInterest(name : String) : URN {
        return URN("")
    }

    private val factories : MutableMap<URN,InterestFactory> = HashMap()

    public fun register(urn:URN, factory:InterestFactory) {
        factories[urn] = factory
    }

    public fun unregister(urn:URN, factory:InterestFactory) {
        factories.remove(urn)
    }
}


trait InterestFactory {
    fun accept(urn :URN):Boolean
    fun create(name : String, config : IntererstConfig) : Interest<*>
}


public trait IntererstConfig {
    val kind : URN
    val owner : URN
    val filter : FilterConfig
    val sorter : SortConfig
    val token : String
}

public trait FilterConfig {

}

public trait SortConfig {

}

