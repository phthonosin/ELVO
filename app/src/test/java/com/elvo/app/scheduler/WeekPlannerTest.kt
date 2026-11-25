package com.elvo.app.scheduler

import org.junit.Assert.assertEquals
import org.junit.Test

class WeekPlannerTest {

    @Test
    fun `splits a narrated weekend into separate untimed tasks`() {
        val plan = WeekPlanner.parse(
            """
            Saturday: wake up at 8, gym for an hour, brunch at 10, study from 12 to 3, walk in the evening, dinner at 7:30
            Sunday: long run at 9, lunch at noon, movie at 4, bed by 11
            """.trimIndent()
        )

        assertEquals(
            listOf(
                6 to listOf("Wake up", "Gym", "Brunch", "Study", "Walk", "Dinner"),
                7 to listOf("Long run", "Lunch", "Movie", "Bed")
            ),
            plan.map { it.dayOfWeek to it.taskLabels }
        )
        assertEquals(listOf(9, 9), plan.map { it.hour })
        assertEquals(listOf(0, 0), plan.map { it.minute })
        assertEquals(false, plan.any { day -> day.tasks.any { it.timing?.canPopFullScreen == true } })
    }

    @Test
    fun `weekend keyword applies to both days`() {
        val plan = WeekPlanner.parse("On the weekend, gym at 9am and dinner at 7pm")
        assertEquals(
            listOf(
                6 to listOf("Gym", "Dinner"),
                7 to listOf("Gym", "Dinner")
            ),
            plan.map { it.dayOfWeek to it.taskLabels }
        )
    }

    @Test
    fun `a vague weekend paragraph becomes a clean day list`() {
        val plan = WeekPlanner.parse(
            "weekend: gym, brunch with friends, study, long run, groceries"
        )
        assertEquals(2, plan.size)
        assertEquals(
            listOf("Gym", "Brunch with friends", "Study", "Long run", "Groceries"),
            plan.first().taskLabels
        )
        assertEquals(false, plan.first().tasks.any { it.timing != null })
    }

    @Test
    fun `newlines split tasks too`() {
        val plan = WeekPlanner.parse("gym\nbrunch with friends\nstudy")
        assertEquals(1, plan.size)
        assertEquals(listOf("Gym", "Brunch with friends", "Study"), plan.first().taskLabels)
    }

    @Test
    fun `only a written time and duration qualify for a full screen alert`() {
        val plan = WeekPlanner.parse("Saturday: gym at 8am for an hour, study from 12 to 3")
        val tasks = plan.first().tasks

        assertEquals(listOf("Gym", "Study"), tasks.map { it.label })
        assertEquals(8, tasks[0].timing?.hour)
        assertEquals(60, tasks[0].timing?.durationMinutes)
        assertEquals(true, tasks[0].timing?.canPopFullScreen)

        assertEquals(12, tasks[1].timing?.hour)
        assertEquals(null, tasks[1].timing?.durationMinutes)
        assertEquals(false, tasks[1].timing?.canPopFullScreen ?: false)
    }

    @Test
    fun `half an hour is understood as a duration`() {
        val plan = WeekPlanner.parse("Sunday: walk at 6pm for half an hour")
        val task = plan.single().tasks.single()
        assertEquals("Walk", task.label)
        assertEquals(18, task.timing?.hour)
        assertEquals(30, task.timing?.durationMinutes)
    }

    @Test
    fun `day range and day list both work`() {
        val range = WeekPlanner.parse("Monday to Friday: gym, reading")
        assertEquals(listOf(1, 2, 3, 4, 5), range.map { it.dayOfWeek })

        val list = WeekPlanner.parse("Saturday and Sunday: long run, meal prep")
        assertEquals(listOf(6, 7), list.map { it.dayOfWeek })
    }

    @Test
    fun `old timed reminders migrate into one untimed day entry`() {
        val plan = WeekPlanner.fromDayLabels(
            listOf(
                6 to "wake up",
                6 to "brunch",
                7 to "long run"
            )
        )
        assertEquals(
            listOf(
                6 to listOf("wake up", "brunch"),
                7 to listOf("long run")
            ),
            plan.map { it.dayOfWeek to it.taskLabels }
        )
        assertEquals(listOf(9, 9), plan.map { it.hour })
    }

    @Test
    fun `the day model keeps user written timings beside each task`() {
        val day = WeekReminder.forDay(
            6,
            listOf(
                ToDoTask("Gym", TaskTiming(8, 0, 60)),
                ToDoTask("Reading")
            )
        )

        assertEquals(listOf("Gym", "Reading"), day.taskLabels)
        assertEquals(8, day.tasks.first().timing?.hour)
        assertEquals(60, day.tasks.first().timing?.durationMinutes)
        assertEquals(null, day.tasks.last().timing)
    }

    @Test
    fun `a task can be marked done without changing its wording`() {
        val day = WeekReminder.forDay(
            2,
            listOf(ToDoTask("Gym"), ToDoTask("Reading"))
        ).withTaskDone(1, true)

        assertEquals(listOf(false, true), day.tasks.map { it.done })
        assertEquals(listOf("Gym", "Reading"), day.taskLabels)
    }
}
