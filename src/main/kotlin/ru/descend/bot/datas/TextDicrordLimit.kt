package ru.descend.bot.datas

class TextDicrordLimit {
    private val textArray = ArrayList<String>()
    private var lastMatchId = ""
    private val delimiter = "\n-----\n"

    fun getAllText() = textArray.sortedByDescending { it.length }
    fun getLastMatchId() = lastMatchId

    fun appendLine(line: String, matchId: String) {
        lastMatchId = matchId
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