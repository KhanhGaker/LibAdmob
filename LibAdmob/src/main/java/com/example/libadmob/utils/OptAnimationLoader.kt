package com.example.libadmob.utils

import android.content.Context
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.util.AttributeSet
import android.util.Xml
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.RotateAnimation
import android.view.animation.ScaleAnimation
import android.view.animation.TranslateAnimation
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException

object OptAnimationLoader {
    @JvmStatic
    @Throws(Resources.NotFoundException::class)
    fun animationLoaded(context: Context, id: Int): Animation? {
        var parser: XmlResourceParser? = null
        try {
            parser = context.resources.getAnimation(id)
            return initAnimFromXml(context, parser)
        } catch (ex: XmlPullParserException) {
            val rnf = Resources.NotFoundException(
                "Can't load animation resource ID #0x" +
                        Integer.toHexString(id)
            )
            rnf.initCause(ex)
            throw rnf
        } catch (ex: IOException) {
            val rnf = Resources.NotFoundException(
                "Can't load animation resource ID #0x" +
                        Integer.toHexString(id)
            )
            rnf.initCause(ex)
            throw rnf
        } finally {
            parser?.close()
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun initAnimFromXml(
        c: Context,
        parser: XmlPullParser,
        parent: AnimationSet? = null,
        attrs: AttributeSet = Xml.asAttributeSet(parser)
    ): Animation? {
        var anim: Animation? = null

        // Make sure we are on a start tag.
        var type: Int
        val depth = parser.depth

        while (((parser.next().also { type = it }) != XmlPullParser.END_TAG || parser.depth > depth)
            && type != XmlPullParser.END_DOCUMENT
        ) {
            if (type != XmlPullParser.START_TAG) {
                continue
            }

            val name = parser.name

            when (name) {
                "set" -> {
                    anim = AnimationSet(c, attrs)
                    initAnimFromXml(c, parser, anim as AnimationSet?, attrs)
                }

                "alpha" -> anim = AlphaAnimation(c, attrs)
                "scale" -> anim = ScaleAnimation(c, attrs)
                "rotate" -> anim = RotateAnimation(c, attrs)
                "translate" -> anim = TranslateAnimation(c, attrs)
                else -> try {
                    anim = Class.forName(name)
                        .getConstructor(Context::class.java, AttributeSet::class.java)
                        .newInstance(c, attrs) as Animation
                } catch (te: Exception) {
                    throw RuntimeException("Unknown animation name: " + parser.name + " error:" + te.message)
                }
            }
            parent?.addAnimation(anim)
        }

        return anim
    }
}