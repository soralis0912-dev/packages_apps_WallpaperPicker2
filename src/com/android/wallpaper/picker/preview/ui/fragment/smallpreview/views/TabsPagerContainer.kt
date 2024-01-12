/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.wallpaper.picker.preview.ui.fragment.smallpreview.views

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.get
import androidx.viewpager.widget.ViewPager

/** This container view wraps {TabsPager}. This is used to control the touch area of {TabsPager}. */
class TabsPagerContainer
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    FrameLayout(context, attrs, defStyleAttr) {

    private var viewPager: ViewPager

    init {
        viewPager = TabsPager(context)

        val params =
            FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
                .apply { gravity = Gravity.CENTER }

        viewPager.clipChildren = false
        viewPager.clipToPadding = false

        viewPager.layoutParams = params
        addView(viewPager)
    }

    // x-coordinate of the beginning of a gesture
    var touchDownX = 0

    // y-coordinate of the beginning of a gesture
    var touchDownY = 0

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        val tab1Rect = viewPager.get(0).getViewRect()
        val tab2Rect = viewPager.get(1).getViewRect()
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                touchDownX = ev.getRawX().toInt()
                touchDownY = ev.getRawY().toInt()

                // if the touch down event doesn't land in the visible rect of either tab buttons
                // then we ignore the entirety of the next gesture
                if (
                    !tab2Rect.contains(touchDownX, touchDownY) &&
                        !tab1Rect.contains(touchDownX, touchDownY)
                ) {
                    return false
                }
            }
        }
        return viewPager.dispatchTouchEvent(ev)
    }

    private fun View.getViewRect(): Rect {
        val returnRect = Rect()
        this.getGlobalVisibleRect(returnRect)
        return returnRect
    }

    fun getViewPager(): ViewPager {
        return viewPager
    }

    override fun performClick(): Boolean {
        // returning true to signify that the click was handled by a custom implementation
        return true
    }
}
