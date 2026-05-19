package com.bayg.data.local.model

data class CalendarEvent(
    val id: Long,
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val isAllDay: Boolean
)
