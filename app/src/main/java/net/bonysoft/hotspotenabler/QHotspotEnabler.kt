package net.bonysoft.hotspotenabler

import android.content.Context
import android.net.ConnectivityManager
import android.os.Handler
import android.util.Log
import com.android.dx.stock.ProxyBuilder
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
            proxy = ProxyBuilder.forClass(classOnStartTetheringCallback())
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
            e.printStackTrace()
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
                    classOnStartTetheringCallback(),
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
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        }

        return
    }

    private fun classOnStartTetheringCallback(): Class<*>? {
        try {
            return Class.forName("android.net.ConnectivityManager\$OnStartTetheringCallback")
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        }

        return null
    }

}
