package com.archimede.w2full.data.mimit

internal object PipeSeparatedTextParser {
    fun parse(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val currentField = StringBuilder()
        var inQuotes = false
        var index = 0

        fun finishField() {
            currentRow += currentField.toString()
            currentField.setLength(0)
        }

        fun finishRow() {
            finishField()
            if (currentRow.any { it.isNotBlank() }) {
                rows += currentRow.toList()
            }
            currentRow.clear()
        }

        while (index < text.length) {
            val char = text[index]
            if (inQuotes) {
                if (char == '"') {
                    val nextIsEscapedQuote = index + 1 < text.length && text[index + 1] == '"'
                    if (nextIsEscapedQuote) {
                        currentField.append('"')
                        index += 1
                    } else {
                        inQuotes = false
                    }
                } else {
                    currentField.append(char)
                }
            } else {
                when (char) {
                    '"' -> {
                        if (currentField.isEmpty()) {
                            inQuotes = true
                        } else {
                            currentField.append(char)
                        }
                    }
                    '|' -> finishField()
                    '\n' -> finishRow()
                    '\r' -> {
                        if (index + 1 < text.length && text[index + 1] == '\n') {
                            index += 1
                        }
                        finishRow()
                    }
                    else -> currentField.append(char)
                }
            }
            index += 1
        }

        check(!inQuotes) { "Unterminated quoted field in pipe-delimited content" }

        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
            finishRow()
        }

        return rows
    }
}
