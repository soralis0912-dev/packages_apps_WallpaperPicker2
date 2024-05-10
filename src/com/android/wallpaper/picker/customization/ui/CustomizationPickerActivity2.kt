/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.wallpaper.picker.customization.ui

import android.graphics.Color
import android.graphics.Point
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.android.wallpaper.R
import com.android.wallpaper.module.MultiPanesChecker
import com.android.wallpaper.picker.customization.ui.view.adapter.PreviewPagerAdapter
import com.android.wallpaper.picker.customization.ui.view.transformer.PreviewPagerPageTransformer
import com.android.wallpaper.util.ActivityUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint(AppCompatActivity::class)
class CustomizationPickerActivity2 : Hilt_CustomizationPickerActivity2() {

    @Inject lateinit var multiPanesChecker: MultiPanesChecker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (
            multiPanesChecker.isMultiPanesEnabled(this) &&
                !ActivityUtils.isLaunchedFromSettingsTrampoline(intent) &&
                !ActivityUtils.isLaunchedFromSettingsRelated(intent)
        ) {
            // If the device supports multi panes, we check if the activity is launched by settings.
            // If not, we need to start an intent to have settings launch the customization
            // activity. In case it is a two-pane situation and the activity should be embedded in
            // the settings app, instead of in the full screen.
            val multiPanesIntent = multiPanesChecker.getMultiPanesIntent(intent)
            ActivityUtils.startActivityForResultSafely(
                this, /* activity */
                multiPanesIntent,
                0, /* requestCode */
            )
            finish()
        }

        setContentView(R.layout.activity_cusomization_picker2)
        initPreviewPager()
    }

    private fun initPreviewPager() {
        val pager = requireViewById<ViewPager2>(R.id.preview_pager)
        pager.apply {
            adapter = PreviewPagerAdapter { viewHolder, position ->
                viewHolder.itemView
                    .requireViewById<View>(R.id.preview_card)
                    .setBackgroundColor(if (position == 0) Color.BLUE else Color.CYAN)
            }
            // Disable over scroll
            (getChildAt(0) as RecyclerView).overScrollMode = RecyclerView.OVER_SCROLL_NEVER
            // The neighboring view should be inflated when pager is rendered
            offscreenPageLimit = 1
            // When pager's height changes, request transform to recalculate the preview offset
            // to make sure correct space between the previews.
            addOnLayoutChangeListener { view, _, _, _, _, _, topWas, _, bottomWas ->
                val isHeightChanged = (bottomWas - topWas) != view.height
                if (isHeightChanged) {
                    pager.requestTransform()
                }
            }
        }

        // Only when pager is laid out, we can get the width and set the preview's offset correctly
        pager.doOnLayout {
            (it as ViewPager2).apply {
                setPageTransformer(PreviewPagerPageTransformer(Point(width, height)))
            }
        }
    }
}
