//package com.bayg.managers
//
//import android.content.Context
//import com.bayg.services.storage.AppDatabase
//import com.bayg.services.storage.Authenticator
//import com.bayg.services.storage.entities.Streak
//import java.time.Instant
//import java.time.LocalDate
//import java.time.ZoneId
//
//class StreakManager(private val context: Context) {
//    private val db by lazy { AppDatabase.getInstance(context) }
//    private val auth by lazy { Authenticator() }
//
//    /**
//     * Determines if a specific day (represented by its index in a Sun-Sat row) is "active".
//     * Now accepts the streak as a parameter to ensure UI reactivity.
//     */
//    fun isActive(dayIndex: Int, streak: Streak?): Boolean {
//        if (streak == null) return false
//        val selectedDate = dayIndexToDate(dayIndex)
//
//        // Don't show future days as active
//        if (selectedDate.isAfter(LocalDate.now())) return false
//
//        // Check if the selected date falls within the current active streak
//        val activeMatch = streak.currentStreakEnd?.takeIf { streak.currentStreakLength > 0 }?.let { raw ->
//            val date = Instant.ofEpochMilli(raw).atZone(ZoneId.systemDefault()).toLocalDate()
//            isWithinRange(selectedDate, streak.currentStreakLength, date)
//        } ?: false
//
//        if (activeMatch) return true
//
//        // Check if it falls within the previous streak (useful for showing "yesterday" after a reset)
//        val previousMatch = streak.lastStreakEnd?.takeIf { streak.lastStreakLength > 0 }?.let { raw ->
//            val date = Instant.ofEpochMilli(raw).atZone(ZoneId.systemDefault()).toLocalDate()
//            isWithinRange(selectedDate, streak.lastStreakLength, date)
//        } ?: false
//
//        return previousMatch
//    }
//
//    private fun isWithinRange(
//        selectedDate: LocalDate,
//        streakLength: Int,
//        lastStreakDate: LocalDate
//    ): Boolean {
//        val firstStreakDate = lastStreakDate.minusDays((streakLength - 1).toLong())
//        return !selectedDate.isBefore(firstStreakDate) && !selectedDate.isAfter(lastStreakDate)
//    }
//
//    /**
//     * Maps a UI day index (0=Sun, ..., 6=Sat) to a LocalDate in the current week.
//     */
//    private fun dayIndexToDate(dayIndex: Int): LocalDate {
//        val today = LocalDate.now()
//        // Convert LocalDate (Mon=1..Sun=7) to Sun=0..Sat=6
//        val todayIndex = today.dayOfWeek.value % 7
//        val diff = (dayIndex - todayIndex).toLong()
//        return today.plusDays(diff)
//    }
//}
