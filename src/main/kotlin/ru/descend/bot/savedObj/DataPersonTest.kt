package ru.descend.bot.savedObj

import dev.kord.common.entity.Snowflake
import dev.kord.core.cache.data.GuildData
import dev.kord.core.entity.Guild
import me.jakejmattson.discordkt.Discord
import org.junit.jupiter.api.Test
import ru.descend.bot.getRandom
import java.io.File

class DataPersonTest {

    @Test
    fun test_create_file(){
        val file = File("src/main/resources/data")
        if (!file.exists()) {
            file.mkdir()
        }
        val fileData = File(file.path + "/53453459345345_data.json")
        if (!fileData.exists()) {
            fileData.createNewFile()
        }
    }

    @Test
    fun test_randomize_int() {
        repeat(100){
            println(getRandom(5))
        }
    }

//    @Test
//    fun writeDataTest() {
//        val data = DataPerson()
//        data.addPersons(Person(123123123u, "Sample name"))
//        data.addPersons(Person(1734573123u, "sadfsfd name").apply {
//            this.pentaKills = 42
//            this.pentaStills["41410924"] = 12
//            this.pentaStills["64357734"] = 2
//        })
//        data.addPersons(Person(957656723u, "Descend"))
//        data.addPersons(Person(53245346u, "Descend"))
//        data.addPersons(Person(77547654656u, "Descend"))
//        writeDataFile(data)
//    }
//
//    @Test
//    fun readDataTest() {
//        val obj = readDataFile()
//        println("Soze: ${obj.listPersons.size}")
//        println(obj)
//
//        println("Find: ${obj.findForUUID("1734573123")?.pentaStills}")
//    }
//
//    @Test
//    fun testAddValues() {
//        val obj = readDataFile()
//
//        println(obj.listPersons.joinToString { it.uid + " " + it.name })
//
//        obj.addPentaStill("957656723")
//        obj.addPentaStill("957656723")
//        obj.addPentaStill("957656723", "53245346")
//        obj.addPentaStill("957656723", "53245346")
//        writeDataFile(obj)
//    }
}