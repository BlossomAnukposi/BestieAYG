package com.bayg.data.local

import android.content.Context
import android.provider.CalendarContract
import com.bayg.data.local.model.CalendarEvent
import java.io.IOException

class CalendarRepository(private val context: Context) {

    fun getUpcomingEvents(daysAhead: Int = 7): Result<List<CalendarEvent>> {
        val events = mutableListOf<CalendarEvent>()

        val now = System.currentTimeMillis()
        val endTime = now + (daysAhead * 24 * 60 * 60 * 1000L)

        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.ALL_DAY
        )

        val selection = "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ?"
        val selectionArgs = arrayOf(now.toString(), endTime.toString())
        val sortOrder = "${CalendarContract.Events.DTSTART} ASC"

        return try {
            val cursor = context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

            cursor?.use {
                while (it.moveToNext()) {
                    val event = CalendarEvent(
                        id = it.getLong(it.getColumnIndexOrThrow(CalendarContract.Events._ID)),
                        title = it.getString(it.getColumnIndexOrThrow(CalendarContract.Events.TITLE)) ?: "Untitled Event",
                        startTime = it.getLong(it.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)),
                        endTime = it.getLong(it.getColumnIndexOrThrow(CalendarContract.Events.DTEND)),
                        isAllDay = it.getInt(it.getColumnIndexOrThrow(CalendarContract.Events.ALL_DAY)) == 1
                    )
                    events.add(event)
                }
            }

            Result.success(events)
        } catch (e: SecurityException) {
            Result.failure(e)
        } catch (e: IOException) {
            Result.failure(e)
        }
    }
}
