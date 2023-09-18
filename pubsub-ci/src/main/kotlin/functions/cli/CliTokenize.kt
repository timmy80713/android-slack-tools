package functions.cli

fun String.tokenizeArgs() = "(?<=\\s|^)([^\"]*?)(?=\\s|$)|(?<=(?:\\s|^)\")([\\s\\S]*?)(?=\"(?:\\s|$))"
    .toRegex()
    .findAll(this)
    .map { it.value }
    .filter { it.isNotBlank() }
    .toList()