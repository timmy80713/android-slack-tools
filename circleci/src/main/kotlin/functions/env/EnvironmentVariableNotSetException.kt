package functions.env

class EnvironmentVariableNotSetException(name: String) : Exception("Environment variable: \"$name\" is not set.")