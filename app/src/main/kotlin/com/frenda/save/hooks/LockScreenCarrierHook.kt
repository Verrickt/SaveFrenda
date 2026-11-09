package com.frenda.save.hooks

import android.view.View
import android.widget.TextView
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

object LockScreenCarrierHook {

    private const val TAG = "[SaveFrenda]"

    fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
        hookCarrierTextManager(lpparam)
        hookLockScreenTextView(lpparam)
    }

    /**
     * 策略 1: 扫描并 Hook 锁屏 CarrierTextController / Manager
     * 针对 HyperOS / MIUI 锁屏运营商控制器做清洗
     */
    private fun hookCarrierTextManager(lpparam: XC_LoadPackage.LoadPackageParam) {
        val candidateClasses = listOf(
            "com.android.keyguard.CarrierTextManager",
            "com.android.keyguard.MiuiCarrierTextManager",
            "com.android.keyguard.MiuiKeyguardCarrierTextViewController",
            "com.android.keyguard.MiuiKeyguardCarrierText"
        )

        for (className in candidateClasses) {
            val clazz = XposedHelpers.findClassIfExists(className, lpparam.classLoader) ?: continue
            XposedBridge.log("$TAG Found carrier manager class: $className")
        }
    }

    /**
     * 策略 2: 拦截 TextView.setText
     * 仅匹配包含锁屏/运营商特征的 View，清洗包含的 "|" 分隔符
     */
    private fun hookLockScreenTextView(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            XposedHelpers.findAndHookMethod(
                TextView::class.java,
                "setText",
                CharSequence::class.java,
                TextView.BufferType::class.java,
                Boolean::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val textView = param.thisObject as? TextView ?: return
                        val text = param.args[0] as? CharSequence ?: return

                        if (!text.contains("|")) return

                        val resName = runCatching {
                            if (textView.id != View.NO_ID && textView.resources != null) {
                                textView.resources.getResourceEntryName(textView.id)
                            } else null
                        }.getOrNull()

                        // 过滤条件：View 命名中包含 carrier 且属于锁屏或状态栏控件
                        val isKeyguardCarrier = resName != null &&
                            resName.contains("carrier", ignoreCase = true) &&
                            (resName.contains("keyguard", ignoreCase = true) ||
                             resName.contains("lock", ignoreCase = true) ||
                             resName == "carrier_text")

                        if (isKeyguardCarrier) {
                            // 将类似 "中国移动 | 中国联通" 中的 " | " 替换为一个空格
                            val cleaned = text.toString()
                                .replace(Regex("\\s*\\|\\s*"), " ")
                                .trim()
                            param.args[0] = cleaned
                            XposedBridge.log("$TAG Successfully cleaned carrier text for [$resName]: '$cleaned'")
                        }
                    }
                }
            )
            XposedBridge.log("$TAG Hooked TextView.setText successfully")
        } catch (t: Throwable) {
            XposedBridge.log("$TAG Failed to hook TextView.setText: ${t.message}")
        }
    }
}
