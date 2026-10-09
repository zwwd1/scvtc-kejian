package com.xiaomanjun.sleepdownschedule.feature.home.day

import com.xiaomanjun.sleepdownschedule.model.*
import org.junit.Assert.*
import org.junit.Test

class CourseWeekScopeTest {
    @Test fun `inferring odd parity from explicit weeks preserves single week edit scope`(){
        val original=CourseEntity(1,"测试课程",null,null,1,listOf(7,8),listOf(3,5),WeekParity.ALL,null)
        assertFalse(courseWeeksChanged(original,original.copy(periods=listOf(1,2),weekParity=WeekParity.ODD)))
        assertTrue(courseWeeksChanged(original,original.copy(weeks=listOf(5))))
    }
}
