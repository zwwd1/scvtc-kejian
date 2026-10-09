package cn.scvtc.campus.core

import kotlin.math.abs
import kotlin.math.sign

/** Accumulate actual user scroll, with separate collapse and expand thresholds. */
class DockScrollIntent {
    private var travel=0f
    fun reset(){travel=0f}
    fun scroll(deltaDp:Float):Boolean? {
        if(!deltaDp.isFinite() || deltaDp==0f)return null
        if(travel.sign!=deltaDp.sign)travel=0f
        travel+=deltaDp
        if(abs(travel)<if(travel<0f)40f else 20f)return null
        val collapse=travel<0f
        travel=0f
        return collapse
    }
}
