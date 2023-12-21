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

package com.android.wallpaper.picker.preview.ui.binder

import android.graphics.Point
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.wallpaper.R
import com.android.wallpaper.model.wallpaper.FoldableDisplay
import com.android.wallpaper.module.CustomizationSections.Screen
import com.android.wallpaper.picker.preview.ui.view.DualDisplayAspectRatioLayout
import com.android.wallpaper.picker.preview.ui.view.DualDisplayAspectRatioLayout.Companion.getViewId
import com.android.wallpaper.picker.preview.ui.viewmodel.WallpaperPreviewViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Binds the dialog on small preview confirming and setting wallpaper with destination. */
object SetWallpaperDialogBinder {
    private val PreviewScreenIds =
        mapOf(
            Screen.LOCK_SCREEN to R.id.lock_preview_selector,
            Screen.HOME_SCREEN to R.id.home_preview_selector
        )

    fun bind(
        dialogContent: View,
        wallpaperPreviewViewModel: WallpaperPreviewViewModel,
        isFoldable: Boolean,
        handheldDisplaySize: Point,
        lifecycleOwner: LifecycleOwner,
        mainScope: CoroutineScope,
        currentNavDestId: Int,
        onFinishActivity: () -> Unit,
        onDismissDialog: () -> Unit,
        navigate: ((View) -> Unit)?,
    ) {
        if (isFoldable)
            bindFoldablePreview(
                dialogContent.requireViewById(R.id.foldable_previews),
                wallpaperPreviewViewModel,
                lifecycleOwner,
                mainScope,
                currentNavDestId,
                navigate,
            )
        else
            bindHandheldPreview(
                dialogContent.requireViewById(R.id.handheld_previews),
                wallpaperPreviewViewModel,
                handheldDisplaySize,
                lifecycleOwner,
                mainScope,
                currentNavDestId,
                navigate,
            )
        val confirmButton = dialogContent.requireViewById<Button>(R.id.button_set)
        val cancelButton = dialogContent.requireViewById<Button>(R.id.button_cancel)
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    wallpaperPreviewViewModel.setWallpaperDialog.collect { dialog ->
                        if (dialog == null) {
                            onDismissDialog.invoke()
                        } else {
                            confirmButton.setOnClickListener {
                                // We on purposely use mainScope (application scope) to launch the
                                // task since the activity can be forced to restart due to the theme
                                // color update from the system wallpaper change.
                                // Application scope can survive the activity restart and continue
                                // subscribing the job.
                                mainScope.launch {
                                    dialog.onConfirmButtonClicked.invoke()
                                    onFinishActivity.invoke()
                                }
                            }
                            cancelButton.setOnClickListener {
                                dialog.onCancelButtonClicked.invoke()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun bindFoldablePreview(
        previewLayout: View,
        wallpaperPreviewViewModel: WallpaperPreviewViewModel,
        lifecycleOwner: LifecycleOwner,
        mainScope: CoroutineScope,
        currentNavDestId: Int,
        navigate: ((View) -> Unit)?,
    ) {
        previewLayout.isVisible = true
        PreviewScreenIds.forEach { screenId ->
            val dualDisplayAspectRatioLayout: DualDisplayAspectRatioLayout =
                previewLayout
                    .requireViewById<FrameLayout>(screenId.value)
                    .requireViewById(R.id.dual_preview)

            dualDisplayAspectRatioLayout.setDisplaySizes(
                mapOf(
                    FoldableDisplay.FOLDED to wallpaperPreviewViewModel.smallerDisplaySize,
                    FoldableDisplay.UNFOLDED to wallpaperPreviewViewModel.wallpaperDisplaySize,
                )
            )
            bindPreviewSelector(previewLayout.requireViewById(screenId.value))
            FoldableDisplay.entries.forEach { display ->
                val previewDisplaySize = dualDisplayAspectRatioLayout.getPreviewDisplaySize(display)
                previewDisplaySize?.let {
                    SmallPreviewBinder.bind(
                        applicationContext = previewLayout.context.applicationContext,
                        view = dualDisplayAspectRatioLayout.requireViewById(display.getViewId()),
                        viewModel = wallpaperPreviewViewModel,
                        mainScope = mainScope,
                        viewLifecycleOwner = lifecycleOwner,
                        screen = screenId.key,
                        displaySize = it,
                        foldableDisplay = display,
                        currentNavDestId = currentNavDestId,
                        navigate = navigate,
                    )
                }
            }
        }
    }

    private fun bindHandheldPreview(
        previewLayout: View,
        wallpaperPreviewViewModel: WallpaperPreviewViewModel,
        displaySize: Point,
        lifecycleOwner: LifecycleOwner,
        mainScope: CoroutineScope,
        currentNavDestId: Int,
        navigate: ((View) -> Unit)?,
    ) {
        previewLayout.isVisible = true
        PreviewScreenIds.forEach { screenId ->
            bindPreviewSelector(previewLayout.requireViewById(screenId.value))
            SmallPreviewBinder.bind(
                applicationContext = previewLayout.context.applicationContext,
                view =
                    previewLayout
                        .requireViewById<FrameLayout>(screenId.value)
                        .requireViewById(R.id.preview),
                viewModel = wallpaperPreviewViewModel,
                screen = screenId.key,
                displaySize = displaySize,
                foldableDisplay = null,
                mainScope = mainScope,
                viewLifecycleOwner = lifecycleOwner,
                currentNavDestId = currentNavDestId,
                navigate = navigate,
            )
        }
    }

    private fun bindPreviewSelector(
        selector: View,
    ) {
        selector.setOnClickListener { selector.isActivated = !selector.isActivated }
    }
}
