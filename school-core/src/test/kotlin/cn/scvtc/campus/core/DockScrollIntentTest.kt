package cn.scvtc.campus.core

import org.junit.Test
import org.junit.Assert.*

class DockScrollIntentTest {
    @Test fun smallMotionDoesNotCollapse(){val i=DockScrollIntent();assertNull(i.scroll(-15f));assertNull(i.scroll(-24f));assertEquals(true,i.scroll(-1f))}
    @Test fun oppositeIntentStartsFromZero(){val i=DockScrollIntent();i.scroll(-39f);assertNull(i.scroll(19f));assertEquals(false,i.scroll(1f))}
    @Test fun thresholdAndPageChangeResetTravel(){val i=DockScrollIntent();assertEquals(true,i.scroll(-40f));assertNull(i.scroll(10f));i.reset();assertNull(i.scroll(19f));assertEquals(false,i.scroll(1f))}
    @Test fun invalidMotionCannotPoisonFutureIntent(){val i=DockScrollIntent();assertNull(i.scroll(Float.NaN));assertNull(i.scroll(Float.POSITIVE_INFINITY));assertEquals(true,i.scroll(-40f))}
}
