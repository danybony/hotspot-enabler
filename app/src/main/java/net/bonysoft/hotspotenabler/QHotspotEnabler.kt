package net.bonysoft.hotspotenabler

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.os.Handler
import android.util.Log
import com.android.dx.stock.ProxyBuilder
import com.crashlytics.android.Crashlytics
import java.io.IOException
import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

class QHotspotEnabler(private val context: Context) : HotspotEnabler {

    override fun enableTethering() {
        setTetheringStatus(context, "startTethering")
    }

    override fun disableTethering() {
        setTetheringStatus(context, "stopTethering")
    }

    private fun setTetheringStatus(context: Context, methodName: String) {
        val outputDir = context.getCodeCacheDir()
        val proxy: Any
        try {
            proxy = ProxyBuilder.forClass(classOnStartTetheringCallback(methodName))
                .dexCache(outputDir).handler(object : InvocationHandler {
                    @Throws(Throwable::class)
                    override operator fun invoke(
                        proxy: Any,
                        method: Method,
                        args: Array<Any>
                    ): Any? {
                        when (method.getName()) {
                            "onTetheringStarted" -> Log.d("tag", "started")
                            "onTetheringFailed" -> Log.d("tag", "failed")
                            else -> ProxyBuilder.callSuper(proxy, method, args)
                        }
                        return null
                    }

                }).build()
        } catch (e: IOException) {
            logToCrashlytics(e, methodName)
            return
        }

        val manager =
            context.getApplicationContext().getSystemService(ConnectivityManager::class.java) as ConnectivityManager

        var method: Method? = null
        try {
            if (methodName.startsWith("start")) {
                method = manager.javaClass.getDeclaredMethod(
                    methodName,
                    Int::class.javaPrimitiveType,
                    Boolean::class.javaPrimitiveType,
                    classOnStartTetheringCallback(methodName),
                    Handler::class.java
                )
                if (method == null) {
                    Log.e("TAG", "$methodName is null")
                } else {
                    method!!.invoke(manager, ConnectivityManager.TYPE_MOBILE, false, proxy, null)
                }
                return
            } else {
                method = manager.javaClass.getDeclaredMethod(
                    methodName,
                    Int::class.javaPrimitiveType
                )
                if (method == null) {
                    Log.e("TAG", "$methodName is null")
                } else {
                    method!!.invoke(manager, ConnectivityManager.TYPE_MOBILE)
                }
                return
            }
        } catch (e: NoSuchMethodException) {
            logToCrashlytics(e, methodName)
        } catch (e: IllegalAccessException) {
            logToCrashlytics(e, methodName)
        } catch (e: InvocationTargetException) {
            logToCrashlytics(e, methodName)
        }

        return
    }

    private fun classOnStartTetheringCallback(methodName: String): Class<*>? {
        try {
            return Class.forName("android.net.ConnectivityManager\$OnStartTetheringCallback")
        } catch (e: ClassNotFoundException) {
            logToCrashlytics(e, methodName)
        }

        return null
    }

    private fun logToCrashlytics(e: Exception, methodName: String) {
        e.printStackTrace()
        Crashlytics.log("${QHotspotEnabler::class.java.simpleName}. Error while setting tethering to:$methodName, SDK:${Build.VERSION.SDK_INT}")
        Crashlytics.logException(e)
    }

}
