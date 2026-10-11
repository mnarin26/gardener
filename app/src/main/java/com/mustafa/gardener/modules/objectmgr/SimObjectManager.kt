package com.mustafa.gardener.modules.objectmgr

import com.mustafa.gardener.core.BaseSimDriver

class SimObjectManager : BaseSimDriver("ObjectManager"), ObjectManager {
    override fun trackedObjectCount(): Int = 0

    override fun detail(): String = "SIM — empty object list"
}
