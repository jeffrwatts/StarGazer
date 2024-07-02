package com.jeffrwatts.stargazer.data.timeoffset

class TimeOffsetRepository {

    private var hourOffset: Long = 0

    fun getTimeOffset(): Long {
        return hourOffset
    }

    fun incrementTime() {
        hourOffset += 1
    }

    fun decrementTime() {
        hourOffset -= 1
    }

    fun resetTime() {
        hourOffset = 0
    }
}
