package io.github.benji377.timety.util.stats

import org.junit.Assert.assertEquals
import org.junit.Test

class ExperienceEngineTest {

    @Test
    fun testCalculateLevel() {
        assertEquals(1, ExperienceEngine.calculateLevel(0))
        assertEquals(1, ExperienceEngine.calculateLevel(99))
        assertEquals(2, ExperienceEngine.calculateLevel(100))
        assertEquals(2, ExperienceEngine.calculateLevel(399))
        assertEquals(3, ExperienceEngine.calculateLevel(400))
        assertEquals(11, ExperienceEngine.calculateLevel(10_000))
    }

    @Test
    fun testGetXpForLevel() {
        assertEquals(0, ExperienceEngine.getXpForLevel(1))
        assertEquals(100, ExperienceEngine.getXpForLevel(2))
        assertEquals(400, ExperienceEngine.getXpForLevel(3))
        assertEquals(8100, ExperienceEngine.getXpForLevel(10))
    }

    @Test
    fun testLevelAndXpForLevelAreConsistent() {
        // The XP threshold of a level must map back to exactly that level.
        for (level in 1..50) {
            assertEquals(level, ExperienceEngine.calculateLevel(ExperienceEngine.getXpForLevel(level)))
        }
    }

    @Test
    fun testGetLevelProgress() {
        assertEquals(0.0, ExperienceEngine.getLevelProgress(0), 1e-9)
        assertEquals(0.5, ExperienceEngine.getLevelProgress(50), 1e-9)
        // Start of level 2: 100 xp into [100, 400)
        assertEquals(0.0, ExperienceEngine.getLevelProgress(100), 1e-9)
        assertEquals(0.5, ExperienceEngine.getLevelProgress(250), 1e-9)
    }

    @Test
    fun testGetTitleThresholds() {
        assertEquals("Novice Planner", ExperienceEngine.getTitle(1))
        assertEquals("Novice Planner", ExperienceEngine.getTitle(4))
        assertEquals("Focus Apprentice", ExperienceEngine.getTitle(5))
        assertEquals("Deep Work Adept", ExperienceEngine.getTitle(10))
        assertEquals("Time Master", ExperienceEngine.getTitle(20))
        assertEquals("Productivity Lord", ExperienceEngine.getTitle(35))
        assertEquals("Timety Legend", ExperienceEngine.getTitle(50))
        assertEquals("Time God", ExperienceEngine.getTitle(100))
    }
}
