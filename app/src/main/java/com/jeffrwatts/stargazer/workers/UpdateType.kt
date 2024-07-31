package com.jeffrwatts.stargazer.workers

enum class UpdateType {
    IMAGE,
    DSO_VARIABLE
}

fun String.toUpdateType(): UpdateType? {
    return enumValues<UpdateType>().firstOrNull { it.name == this }
}