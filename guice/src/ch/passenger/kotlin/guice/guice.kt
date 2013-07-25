package ch.passenger.kotlin.guice

import com.google.inject.Binder
import com.google.inject.binder.AnnotatedBindingBuilder
import com.google.inject.binder.ScopedBindingBuilder
import com.google.inject.Injector
import java.util.ArrayList
import com.google.inject.Module
import com.google.inject.Guice
import com.google.inject.binder.LinkedBindingBuilder
import com.google.inject.servlet.ServletModule

/**
 * Created by sdju on 24.07.13.
 * cf http://java.dzone.com/articles/kotlin-guice-example
 */
fun<T> Binder.bind() = bind(javaClass<T>())!!

fun<T> AnnotatedBindingBuilder<in T>.to() = to(javaClass<T>())!!
fun<T> LinkedBindingBuilder<in T>.to() = to(javaClass<T>())!!

fun<T> Injector.getInstance() = getInstance(javaClass<T>())

fun injector(config: ModuleCollector.() -> Any?) : Injector {
    val collector = ModuleCollector()
    collector.config()
    return Guice.createInjector(collector.collected)!!
}

class ModuleCollector() {
    val collected = ArrayList<Module>()

    fun module(config: Binder.()->Any?) : Module = object : Module {
        override fun configure(binder : Binder?) {
            binder!!.config()
        }
    }

    fun Module.plus() {
        collected.add(this)
    }
}