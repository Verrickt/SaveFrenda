package com.frenda.save.hooks

import android.view.View
import android.widget.TextView
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

object LockScreenCarrierHook {

    private const val TAG = "[SaveFrenda]"

    // 方案 B：去除竖线及两端空格，直接紧凑拼接（例如：中国移动中国联通）
    private val SEPARATOR_REGEX = Regex("\\s*\\|\\s*")

    fun init(lpparam: XC_LoadPackage.LoadPackageParam) {
        val classLoader = lpparam.classLoader

        // 1. Hook 锁屏 CarrierTextInjector 的回调（精准拦截锁屏状态栏 CarrierText）
        hookCarrierTextInjectorCallback(classLoader)

        // 2. Hook 锁屏时钟容器 KeyguardClockContainer 的回调（同步清洗 constant_carrier_info）
        hookKeyguardClockCallback(classLoader)

        // 3. Hook com.android.keyguard.CarrierText 控件层（防御式兜底）
        hookCarrierTextView(classLoader)

        // 4. Hook 全局锁屏 TextView.setText 带有 carrier_text ID 的控件（兜底保障）
        hookTextViewFallback()
    }

    private fun cleanCarrierText(text: CharSequence?): String? {
        if (text == null || !text.contains("|")) return null
        return text.toString().replace(SEPARATOR_REGEX, "")
    }

    /**
     * 目标 1: com.android.systemui.statusbar.ui.viewmodel.CarrierTextInjector$carrierTextCallback$1
     * HyperOS 锁屏状态栏 CarrierText 专属的数据监听注入器
     */
    private fun hookCarrierTextInjectorCallback(classLoader: ClassLoader) {
        val targetClass = "com.android.systemui.statusbar.ui.viewmodel.CarrierTextInjector\$carrierTextCallback$1"
        val clazz = XposedHelpers.findClassIfExists(targetClass, classLoader) ?: return

        val hook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                // onCarrierTextChanged(int status, String text) -> args[1]
                // onCarrierTextChanged(int slot, int status, String text) -> args[2]
                val strIndex = if (param.args.size == 2) 1 else if (param.args.size == 3) 2 else -1
                if (strIndex >= 0) {
                    val original = param.args[strIndex] as? String
                    cleanCarrierText(original)?.let { cleaned ->
                        param.args[strIndex] = cleaned
                        XposedBridge.log("$TAG [CarrierTextInjector] Cleaned: '$original' -> '$cleaned'")
                    }
                }
            }
        }

        runCatching {
            XposedHelpers.findAndHookMethod(clazz, "onCarrierTextChanged", Int::class.javaPrimitiveType, String::class.java, hook)
            XposedBridge.log("$TAG Hooked CarrierTextInjector callback (2 args)")
        }.onFailure { XposedBridge.log("$TAG Failed hooking CarrierTextInjector (2 args): ${it.message}") }

        runCatching {
            XposedHelpers.findAndHookMethod(clazz, "onCarrierTextChanged", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, String::class.java, hook)
            XposedBridge.log("$TAG Hooked CarrierTextInjector callback (3 args)")
        }.onFailure { XposedBridge.log("$TAG Failed hooking CarrierTextInjector (3 args): ${it.message}") }
    }

    /**
     * 目标 2: com.android.keyguard.clock.KeyguardClockContainer$carrierTextCallback$1
     * 写入 Settings.Global "constant_carrier_info" 的锁屏时钟组件监听器
     */
    private fun hookKeyguardClockCallback(classLoader: ClassLoader) {
        val targetClass = "com.android.keyguard.clock.KeyguardClockContainer\$carrierTextCallback$1"
        val clazz = XposedHelpers.findClassIfExists(targetClass, classLoader) ?: return

        val hook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val strIndex = if (param.args.size == 2) 1 else if (param.args.size == 3) 2 else -1
                if (strIndex >= 0) {
                    val original = param.args[strIndex] as? String
                    cleanCarrierText(original)?.let { cleaned ->
                        param.args[strIndex] = cleaned
                        XposedBridge.log("$TAG [KeyguardClock] Cleaned: '$original' -> '$cleaned'")
                    }
                }
            }
        }

        runCatching {
            XposedHelpers.findAndHookMethod(clazz, "onCarrierTextChanged", Int::class.javaPrimitiveType, String::class.java, hook)
            XposedBridge.log("$TAG Hooked KeyguardClockContainer callback (2 args)")
        }

        runCatching {
            XposedHelpers.findAndHookMethod(clazz, "onCarrierTextChanged", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, String::class.java, hook)
            XposedBridge.log("$TAG Hooked KeyguardClockContainer callback (3 args)")
        }
    }

    /**
     * 目标 3: com.android.keyguard.CarrierText
     * HyperOS 锁屏专用的 CarrierText 自定义 TextView
     */
    private fun hookCarrierTextView(classLoader: ClassLoader) {
        val carrierTextClass = XposedHelpers.findClassIfExists("com.android.keyguard.CarrierText", classLoader) ?: return

        val setTextHook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val original = param.args[0] as? CharSequence ?: return
                cleanCarrierText(original)?.let { cleaned ->
                    param.args[0] = cleaned
                    XposedBridge.log("$TAG [CarrierText View] Cleaned: '$original' -> '$cleaned'")
                }
            }
        }

        runCatching {
            XposedHelpers.findAndHookMethod(
                carrierTextClass,
                "setText",
                CharSequence::class.java,
                TextView.BufferType::class.java,
                Boolean::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                setTextHook
            )
            XposedBridge.log("$TAG Hooked com.android.keyguard.CarrierText.setText(4 args)")
        }

        runCatching {
            XposedHelpers.findAndHookMethod(
                carrierTextClass,
                "setText",
                CharSequence::class.java,
                TextView.BufferType::class.java,
                setTextHook
            )
            XposedBridge.log("$TAG Hooked com.android.keyguard.CarrierText.setText(2 args)")
        }
    }

    /**
     * 目标 4: 全局 TextView.setText 兜底
     * 严格限制：只对 ID 为 keyguard_carrier_text / carrier_text 或包含 keyguard/lock 的运营商控件生效
     */
    private fun hookTextViewFallback() {
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
                        val original = param.args[0] as? CharSequence ?: return

                        if (!original.contains("|")) return

                        val resName = runCatching {
                            if (textView.id != View.NO_ID && textView.resources != null) {
                                textView.resources.getResourceEntryName(textView.id)
                            } else null
                        }.getOrNull()

                        val isKeyguardCarrier = resName != null &&
                            resName.contains("carrier", ignoreCase = true) &&
                            (resName.contains("keyguard", ignoreCase = true) ||
                             resName.contains("lock", ignoreCase = true) ||
                             resName == "carrier_text")

                        if (isKeyguardCarrier) {
                            cleanCarrierText(original)?.let { cleaned ->
                                param.args[0] = cleaned
                                XposedBridge.log("$TAG [TextView Fallback id:$resName] Cleaned: '$original' -> '$cleaned'")
                            }
                        }
                    }
                }
            )
            XposedBridge.log("$TAG Hooked TextView.setText fallback successfully")
        } catch (t: Throwable) {
            XposedBridge.log("$TAG Failed to hook TextView.setText: ${t.message}")
        }
    }
}
