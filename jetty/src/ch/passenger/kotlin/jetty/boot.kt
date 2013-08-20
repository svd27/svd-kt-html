package ch.passenger.kotlin.jetty

import java.io.File

/**
 * Created by sdju on 20.08.13.
 */
fun main(args:Array<String>) {
    val f = File("out/artifacts/jstest")
    println("${f.getAbsolutePath()}")
    f.list()?.forEach { println("${it}") }
    val cfg = readAppCfg(File(f, "bosork.json"))
    startApp(cfg)
}