package ch.passenger.kotlin.html.js.model

import ch.passenger.kotlin.html.js.html.TableModel

/**
 * Created with IntelliJ IDEA.
 * User: sdju
 * Date: 09.07.13
 * Time: 15:39
 * To change this template use File | Settings | File Templates.
 */
trait Identifiable {
    val id : Long
}

trait Stated {
    var loadState : String
}

native class Word : Identifiable,Stated {
    var name : String = js.noImpl
    override val id : Long = js.noImpl
    var qualities : Array<Long> = js.noImpl
    var kind : Long = js.noImpl
    var clazz : String = js.noImpl
    override var loadState: String = js.noImpl
    var qualityLabel = ""
    var description : String = js.noImpl
}

class WordTableModel : TableModel<Word>() {
    {
        columns.add("id")
        columns.add("name")
        columns.add("kind")
        columns.add("description")
    }
    override fun value(t: Word, col: String): Any? {
        if(t.loadState.equals("PENDING")) return null
        var v :Any?= null
        when(col) {
            "id" -> v = t.id
            "name" -> v = t.name
            "kind" -> v = t.kind
            "description" -> v = t.description
            "loadState" -> v = t.loadState
            else -> ""
        }

        return v
    }
    override fun value(t: Word, col: String, v: Any?) {
        when(col) {
            "id" -> throw IllegalStateException()
            "name" -> {
                if(v!=null)
                    t.name = v as String
                else throw IllegalStateException()
            }
            "kind" -> t.kind = v as Long
            "description" -> t.description = v?.toString()!!
            "loadState" -> throw IllegalStateException()
            else -> ""
        }
    }
}



