package com.frenda.save

import com.frenda.save.hooks.LockScreenCarrierHook
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage

class MainHook : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // 严格限制作用域，只在 SystemUI 进程中执行
        if (lpparam.packageName != "com.android.systemui") {
            return
        }

        XposedBridge.log("[SaveFrenda] SystemUI loaded, initializing hooks...")
        try {
            LockScreenCarrierHook.init(lpparam)
        } catch (t: Throwable) {
            XposedBridge.log("[SaveFrenda] Failed to initialize LockScreenCarrierHook: ${t.message}")
        }
    }
}
