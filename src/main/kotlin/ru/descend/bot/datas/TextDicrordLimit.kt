package ru.descend.bot.datas

class TextDicrordLimit {
    private val textArray = ArrayList<String>()
    private var lastMatchId = ""
    private var lastMatchCode = ""
    private val delimiter = "\n-----\n"

    fun getAllText() = textArray.reversed()
    fun getLastMatchId() = lastMatchId
    fun getLastMatchCode() = lastMatchCode

    fun appendLine(line: String, matchId: String, matchCode: String) {
        lastMatchId = matchId
        lastMatchCode = matchCode
        val lastLine = textArray.findLast { it.length < 1900 }
        if (lastLine != null) {
            textArray[textArray.indexOf(lastLine)] = lastLine + line
        } else {
            textArray.add(delimiter + line)
        }
    }

    fun clear() {
        textArray.clear()
    }
}