package com.elvo.app.scheduler

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** A start time and/or duration the user actually wrote, never an invented slot. */
data class TaskTiming(
    val hour: Int?,
    val minute: Int,
    val durationMinutes: Int?
) {
    val canPopFullScreen: Boolean get() = hour != null && durationMinutes != null

    fun displayLabel(): String? {
        val parts = mutableListOf<String>()
        if (hour != null) {
            val shown = when (hour % 12) {
                0 -> 12
                else -> hour % 12
            }
            parts += "%d:%02d %s".format(shown, minute, if (hour < 12) "am" else "pm")
        }
        if (durationMinutes != null) {
            parts += if (durationMinutes >= 60 && durationMinutes % 60 == 0) {
                "${durationMinutes / 60}h"
            } else {
                "${durationMinutes}m"
            }
        }
        return parts.joinToString(" · ").ifEmpty { null }
    }
}

data class ToDoTask(
    val label: String,
    val timing: TaskTiming? = null,
    val done: Boolean = false
)

/**
 * One day of the to-do list. [label] holds that day's tasks, one per line.
 * The time is a single gentle daily reminder, not a slot for any one task.
 */
data class WeekReminder(
    val dayOfWeek: Int,
    val hour: Int,
    val minute: Int,
    val label: String,
    val taskTimings: List<TaskTiming?> = emptyList(),
    val taskDone: List<Boolean> = emptyList()
) {
    val requestCode: Int get() = dayOfWeek * 10000 + hour * 100 + minute

    val tasks: List<ToDoTask>
        get() = label.split('\n')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapIndexed { index, line ->
                ToDoTask(line, taskTimings.getOrNull(index), taskDone.getOrNull(index) == true)
            }

    val taskLabels: List<String> get() = tasks.map { it.label }

    fun withTaskDone(index: Int, done: Boolean): WeekReminder {
        val current = taskDone.toMutableList()
        repeat(tasks.size) { position ->
            while (current.size <= position) current.add(false)
        }
        if (index in current.indices) current[index] = done
        return copy(taskDone = current)
    }

    fun dayName(): String = DAYS.getOrNull(dayOfWeek - 1) ?: "Monday"

    companion object {
        val DAYS = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        const val DAILY_REMINDER_HOUR = 9
        const val DAILY_REMINDER_MINUTE = 0

        fun forDay(day: Int, tasks: List<ToDoTask>): WeekReminder {
            val clean = tasks
                .map { it.copy(label = it.label.trim()) }
                .filter { it.label.isNotEmpty() }
                .distinctBy { it.label.lowercase() }
            return WeekReminder(
                dayOfWeek = day,
                hour = DAILY_REMINDER_HOUR,
                minute = DAILY_REMINDER_MINUTE,
                label = clean.joinToString("\n") { it.label },
                taskTimings = clean.map { it.timing },
                taskDone = clean.map { it.done }
            )
        }

        fun forLabels(day: Int, labels: List<String>): WeekReminder =
            forDay(day, labels.map { ToDoTask(it) })
    }
}

/**
 * Turns a plain description of somebody's week into a clean to-do list.
 *
 * Lines, commas and natural connectors split tasks. Day names, day ranges,
 * "weekend" and "every day" decide when each set of tasks belongs. Times and
 * durations are kept only when the user writes them; they never get invented.
 */
object WeekPlanner {

    private val dayWords = "monday|tuesday|wednesday|thursday|friday|saturday|sunday|mon|tues|tue|wed|thurs|thur|thu|fri|sat|sun"
    private val dayRegex = Regex("\\b($dayWords)\\b", RegexOption.IGNORE_CASE)
    private val dayRangeRegex = Regex(
        "\\b($dayWords)\\b\\s*(?:-|–|—|to|through)\\s*\\b($dayWords)\\b",
        RegexOption.IGNORE_CASE
    )
    private val everyDayRegex = Regex("\\b(every\\s*day|daily|all\\s*week)\\b", RegexOption.IGNORE_CASE)
    private val weekendRegex = Regex(
        "\\b(?:this\\s+weekend|(?:on\\s+(?:the\\s+)?)?weekends?)\\b",
        RegexOption.IGNORE_CASE
    )

    private val dayPrefixRegex = Regex(
        """^\s*((?:$dayWords)(?:\s*(?:,|and|&|/|-|–|to|through)\s*(?:$dayWords))*)\s*(?:[:–-]|(?=\s))\s*""",
        RegexOption.IGNORE_CASE
    )
    private val dayBoundaryRegex = Regex(
        """(?:[.;!?]|\bthen\b|\bafter that\b)\s+(?=(?:$dayWords|weekends?|every\s*day|daily|all\s*week)\b)|(?<=,)\s+(?=(?:$dayWords|weekends?|every\s*day|daily|all\s*week)\b)""",
        RegexOption.IGNORE_CASE
    )
    private val taskSplitRegex = Regex(
        """\s*(?:\band then\b|\bafter that\b|\bafterwards\b|\bnext\b|\bthen\b|\bplus\b|\balso\b|\band\b|,|;|\|)\s*""",
        RegexOption.IGNORE_CASE
    )

    private val intentPrefixRegex = Regex(
        """^\s*(?:(?:i(?:'ll| will|'m going to| am going to| have to| need to| want to| wanna| plan to| should| might| maybe| probably)?|we(?:'ll| will|'re going to)?|let'?s|please|need to|have to|going to|gonna|plan to)\s+)+""",
        RegexOption.IGNORE_CASE
    )
    private val leadingFillerRegex = Regex(
        """^\s*(?:in|on|at|around|by|from|to|for|with|the|a|an|some|just|please)\s+""",
        RegexOption.IGNORE_CASE
    )
    private val trailingFillerRegex = Regex(
        """\s+(?:in|on|at|around|by|from|to|for|with|the|a|an|some|just|please)$""",
        RegexOption.IGNORE_CASE
    )
    private val whitespaceRegex = Regex("\\s+")
    private val edgePunctuation = charArrayOf(',', ';', '.', ':', '!', '?', '-', '–', '—', '|')

    private val fromTimeRegex = Regex(
        """\bfrom\s+(\d{1,2})(?::(\d{2}))?\s*(a\.?m\.?|p\.?m\.?)?\b""",
        RegexOption.IGNORE_CASE
    )
    private val meridiemTimeRegex = Regex(
        """\b(\d{1,2})(?::(\d{2}))?\s*(a\.?m\.?|p\.?m\.?)\b""",
        RegexOption.IGNORE_CASE
    )
    private val twentyFourTimeRegex = Regex("""\b(\d{1,2}):(\d{2})\b""")
    private val atTimeRegex = Regex(
        """\b(?:at|around|by)\s+(\d{1,2})(?::(\d{2}))?\s*(a\.?m\.?|p\.?m\.?)?\b""",
        RegexOption.IGNORE_CASE
    )
    private val explicitDurationRegex = Regex(
        """\b(?:for|in)\s+(an?|one|two|three|four|half|\d+(?:\.\d+)?)\s*(hours?|hrs?|minutes?|mins?)\b""",
        RegexOption.IGNORE_CASE
    )
    private val bareDurationRegex = Regex(
        """\b(an?|one|two|three|four|half|\d+(?:\.\d+)?)\s*(hours?|hrs?|minutes?|mins?)\b""",
        RegexOption.IGNORE_CASE
    )
    private val halfHourRegex = Regex(
        """\b(?:half\s+an?\s+hour|half\s+hour)\b""",
        RegexOption.IGNORE_CASE
    )

    /** Time words are stripped from labels; only explicit clock times are kept. */
    private val timePatterns = listOf(
        Regex(
            """\b(?:at|around|by|before|after)\s+\d{1,2}(?::\d{2})?\s*(?:a\.?m\.?|p\.?m\.?)?\b""",
            RegexOption.IGNORE_CASE
        ),
        Regex(
            """\bbetween\s+\d{1,2}(?::\d{2})?\s*(?:a\.?m\.?|p\.?m\.?)?\s*(?:and|to|-|–)\s*\d{1,2}(?::\d{2})?\s*(?:a\.?m\.?|p\.?m\.?)?\b""",
            RegexOption.IGNORE_CASE
        ),
        Regex(
            """\bfrom\s+\d{1,2}(?::\d{2})?\s*(?:a\.?m\.?|p\.?m\.?)?\s*(?:to|until|till|-|–)\s*\d{1,2}(?::\d{2})?\s*(?:a\.?m\.?|p\.?m\.?)?\b""",
            RegexOption.IGNORE_CASE
        ),
        Regex("""\b\d{1,2}(?::\d{2})?\s*(?:a\.?m\.?|p\.?m\.?)\b""", RegexOption.IGNORE_CASE),
        Regex("""\b\d{1,2}:\d{2}\b"""),
        explicitDurationRegex,
        halfHourRegex,
        bareDurationRegex,
        Regex(
            """\b(?:morning|afternoon|evening|night|noon|midnight|tonight|today|tomorrow)\b""",
            RegexOption.IGNORE_CASE
        )
    )

    fun parse(description: String): List<WeekReminder> {
        val byDay = mutableMapOf<Int, MutableList<ToDoTask>>()
        var activeDays: Set<Int> = emptySet()

        description.replace('\r', '\n').lines()
            .flatMap { splitDayChunks(it) }
            .forEach { chunk ->
                val (days, text) = resolveDays(chunk, activeDays)
                if (days.isNotEmpty()) activeDays = days
                val tasks = extractTasks(text)
                days.forEach { day ->
                    byDay.getOrPut(day) { mutableListOf() }.addAll(tasks)
                }
            }

        return byDay.map { (day, tasks) -> WeekReminder.forDay(day, tasks) }
            .filter { it.tasks.isNotEmpty() }
            .sortedBy { it.dayOfWeek }
    }

    private fun splitDayChunks(line: String): List<String> =
        dayBoundaryRegex.replace(line, "\n")
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    private fun resolveDays(chunk: String, activeDays: Set<Int>): Pair<Set<Int>, String> {
        var text = chunk.trim()
        var days: Set<Int>? = null

        dayPrefixRegex.find(text)?.let { match ->
            days = daysFromGroup(match.groupValues[1])
            text = text.removeRange(match.range).trim()
        }

        if (days == null) {
            val inlineDays = dayRegex.findAll(text)
                .mapNotNull { dayNumber(it.value) }
                .toSet()
            if (inlineDays.isNotEmpty()) {
                days = inlineDays
                text = dayRegex.replace(text, " ")
            }
        }

        if (days == null) {
            when {
                everyDayRegex.containsMatchIn(text) -> {
                    days = (1..7).toSet()
                    text = everyDayRegex.replace(text, " ")
                }
                weekendRegex.containsMatchIn(text) -> {
                    days = setOf(6, 7)
                    text = weekendRegex.replace(text, " ")
                }
                else -> days = activeDays.ifEmpty { setOf(LocalDate.now().dayOfWeek.value) }
            }
        } else {
            text = everyDayRegex.replace(text, " ")
            text = weekendRegex.replace(text, " ")
        }

        return days!! to text
    }

    private fun daysFromGroup(group: String): Set<Int>? {
        val lower = group.lowercase()
        dayRangeRegex.find(lower)?.let { match ->
            val from = dayNumber(match.groupValues[1])
            val to = dayNumber(match.groupValues[2])
            if (from != null && to != null) return daysBetween(from, to)
        }
        val days = dayRegex.findAll(lower).mapNotNull { dayNumber(it.value) }.toSet()
        return days.ifEmpty { null }
    }

    private fun daysBetween(from: Int, to: Int): Set<Int> {
        val days = mutableSetOf<Int>()
        var day = from
        while (true) {
            days += day
            if (day == to) break
            day = if (day == 7) 1 else day + 1
        }
        return days
    }

    private fun extractTasks(text: String): List<ToDoTask> =
        text.split(taskSplitRegex)
            .mapNotNull { segment ->
                val label = cleanTask(segment)
                if (label.length < 3 || label.equals("reminder", ignoreCase = true)) return@mapNotNull null
                val time = parseTime(segment)
                val duration = parseDuration(segment)
                val timing = if (time != null || duration != null) {
                    TaskTiming(time?.first, time?.second ?: 0, duration)
                } else null
                ToDoTask(label, timing)
            }
            .distinctBy { it.label.lowercase() }

    private fun parseTime(segment: String): Pair<Int, Int>? {
        val lower = segment.lowercase()
        var raw: String? = null
        var minute = 0
        var suffix = ""

        fromTimeRegex.find(lower)?.let { match ->
            raw = match.groupValues[1]
            minute = match.groupValues[2].toIntOrNull() ?: 0
            suffix = match.groupValues[3]
        }
        if (raw == null) meridiemTimeRegex.find(lower)?.let { match ->
            raw = match.groupValues[1]
            minute = match.groupValues[2].toIntOrNull() ?: 0
            suffix = match.groupValues[3]
        }
        if (raw == null) twentyFourTimeRegex.find(lower)?.let { match ->
            raw = match.groupValues[1]
            minute = match.groupValues[2].toIntOrNull() ?: 0
        }
        if (raw == null) atTimeRegex.find(lower)?.let { match ->
            raw = match.groupValues[1]
            minute = match.groupValues[2].toIntOrNull() ?: 0
            suffix = match.groupValues[3]
        }

        val value = raw?.toIntOrNull() ?: return null
        val hour = resolveHour(value, suffix) ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return hour to minute
    }

    private fun resolveHour(value: Int, suffix: String): Int? {
        if (value !in 0..23) return null
        if (suffix.startsWith('p', ignoreCase = true)) return value % 12 + 12
        if (suffix.startsWith('a', ignoreCase = true)) return value % 12
        return value
    }

    private fun parseDuration(segment: String): Int? {
        val lower = segment.lowercase()
        if (halfHourRegex.containsMatchIn(lower)) return 30
        val match = explicitDurationRegex.find(lower) ?: bareDurationRegex.find(lower) ?: return null
        val amount = when (val word = match.groupValues[1]) {
            "a", "an", "one" -> 1.0
            "two" -> 2.0
            "three" -> 3.0
            "four" -> 4.0
            "half" -> 0.5
            else -> word.toDoubleOrNull() ?: return null
        }
        val minutes = if (match.groupValues[2].startsWith('h')) amount * 60 else amount
        return if (minutes in 1.0..720.0) minutes.toInt() else null
    }

    private fun cleanTask(raw: String): String {
        var text = raw.trim().trim(*edgePunctuation)
        text = intentPrefixRegex.replace(text, " ")
        timePatterns.forEach { pattern -> text = pattern.replace(text, " ") }
        text = dayRegex.replace(text, " ")
        text = weekendRegex.replace(text, " ")
        text = everyDayRegex.replace(text, " ")
        repeat(2) {
            text = text.trim()
            text = leadingFillerRegex.replace(text, " ").trim()
            text = trailingFillerRegex.replace(text, " ").trim()
        }
        text = whitespaceRegex.replace(text, " ").trim().trim(*edgePunctuation)
        return text.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    private fun dayNumber(word: String): Int? = when (word.lowercase().take(3)) {
        "mon" -> 1
        "tue" -> 2
        "wed" -> 3
        "thu" -> 4
        "fri" -> 5
        "sat" -> 6
        "sun" -> 7
        else -> null
    }

    fun toJson(plan: List<WeekReminder>): String = JSONArray().apply {
        plan.forEach { item ->
            put(JSONObject().apply {
                put("d", item.dayOfWeek)
                put("h", item.hour)
                put("m", item.minute)
                put("t", item.label)
                put("p", JSONArray().apply {
                    item.tasks.forEach { task ->
                        val timing = task.timing
                        put(
                            if (timing == null) JSONObject.NULL else JSONObject().apply {
                                if (timing.hour == null) put("h", JSONObject.NULL) else put("h", timing.hour)
                                put("m", timing.minute)
                                if (timing.durationMinutes == null) {
                                    put("d", JSONObject.NULL)
                                } else {
                                    put("d", timing.durationMinutes)
                                }
                            }
                        )
                    }
                })
                put("x", JSONArray().apply {
                    item.tasks.forEach { task -> put(task.done) }
                })
            })
        }
    }.toString()

    /** Collapses any stored labels into one day-level entry per day. */
    fun fromDayLabels(entries: List<Pair<Int, String>>): List<WeekReminder> {
        val byDay = mutableMapOf<Int, MutableList<String>>()
        entries.forEach { (day, label) ->
            if (day in 1..7 && label.isNotBlank()) {
                byDay.getOrPut(day) { mutableListOf() }.addAll(
                    label.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
                )
            }
        }
        return byDay.map { (day, labels) -> WeekReminder.forLabels(day, labels) }
            .filter { it.tasks.isNotEmpty() }
            .sortedBy { it.dayOfWeek }
    }

    /** Reads both the new day-level list and reminders saved by older builds. */
    fun fromJson(json: String): List<WeekReminder> = runCatching {
        val array = JSONArray(json)
        (0 until array.length()).mapNotNull { index ->
            val item = array.getJSONObject(index)
            val day = item.optInt("d", 0)
            val label = item.optString("t", "")
            if (day !in 1..7 || label.isBlank()) return@mapNotNull null

            val timings = item.optJSONArray("p")?.let { encoded ->
                (0 until encoded.length()).map { taskIndex ->
                    if (encoded.isNull(taskIndex)) {
                        null
                    } else {
                        val value = encoded.getJSONObject(taskIndex)
                        TaskTiming(
                            hour = if (value.isNull("h")) null else value.getInt("h"),
                            minute = value.optInt("m", 0),
                            durationMinutes = if (value.isNull("d")) null else value.getInt("d")
                        )
                    }
                }
            }.orEmpty()

            val done = item.optJSONArray("x")?.let { encoded ->
                (0 until encoded.length()).map { encoded.optBoolean(it, false) }
            }.orEmpty()

            WeekReminder(
                dayOfWeek = day,
                hour = WeekReminder.DAILY_REMINDER_HOUR,
                minute = WeekReminder.DAILY_REMINDER_MINUTE,
                label = label,
                taskTimings = timings,
                taskDone = done
            )
        }.filter { it.tasks.isNotEmpty() }
            .sortedBy { it.dayOfWeek }
    }.getOrDefault(emptyList())
}
