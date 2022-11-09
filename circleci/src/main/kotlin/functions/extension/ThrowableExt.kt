package functions.extension

val Throwable.debugMessage get() = "${javaClass.name}: $message"