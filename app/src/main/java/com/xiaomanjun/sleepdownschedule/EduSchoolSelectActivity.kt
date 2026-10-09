package com.xiaomanjun.sleepdownschedule

import com.xiaomanjun.sleepdownschedule.app.ui.EduSchoolSelectActivityHost

open class EduSchoolSelectActivity : androidx.activity.ComponentActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(android.content.Intent(this, cn.scvtc.campus.ScvtcLoginActivity::class.java))
        finish()
    }
}
