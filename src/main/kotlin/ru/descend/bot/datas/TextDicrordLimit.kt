package ru.descend.bot.datas

data class TextDicrordString (
    val text: String,
    val date: Long = System.currentTimeMillis()
)

class TextDicrordLimit {
    private val textArray = ArrayList<TextDicrordString>()
    private val delimiter = "\n-----\n"

    fun getAllText() = textArray.sortedBy { it.date }.map { it.text }

    fun appendLine(line: String) {
        val lastLine = textArray.findLast { it.text.length < (1990 - line.length) }
        if (lastLine != null) {
            textArray[textArray.indexOf(lastLine)] = TextDicrordString(lastLine.text + line)
        } else {
            textArray.add(TextDicrordString(delimiter + line))
        }
    }

    fun clear() {
        textArray.clear()
    }
}