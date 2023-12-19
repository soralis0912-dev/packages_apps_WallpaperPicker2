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
package com.android.wallpaper.picker.preview.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.android.wallpaper.model.wallpaper.FoldableDisplay
import com.android.wallpaper.model.wallpaper.ScreenOrientation
import com.android.wallpaper.model.wallpaper.WallpaperModel
import com.android.wallpaper.module.CustomizationSections.Screen
import com.android.wallpaper.picker.di.modules.PreviewUtilsModule.HomeScreenPreviewUtils
import com.android.wallpaper.picker.di.modules.PreviewUtilsModule.LockScreenPreviewUtils
import com.android.wallpaper.picker.preview.domain.interactor.WallpaperPreviewInteractor
import com.android.wallpaper.picker.preview.ui.WallpaperPreviewActivity
import com.android.wallpaper.util.DisplayUtils
import com.android.wallpaper.util.PreviewUtils
import com.android.wallpaper.util.WallpaperConnection.WhichPreview
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

/** Top level [ViewModel] for [WallpaperPreviewActivity] and its fragments */
@HiltViewModel
class WallpaperPreviewViewModel
@Inject
constructor(
    interactor: WallpaperPreviewInteractor,
    val staticWallpaperPreviewViewModel: StaticWallpaperPreviewViewModel,
    val previewActionsViewModel: PreviewActionsViewModel,
    private val displayUtils: DisplayUtils,
    @HomeScreenPreviewUtils private val homePreviewUtils: PreviewUtils,
    @LockScreenPreviewUtils private val lockPreviewUtils: PreviewUtils,
) : ViewModel() {

    val smallerDisplaySize = displayUtils.getRealSize(displayUtils.getSmallerDisplay())
    val wallpaperDisplaySize = displayUtils.getRealSize(displayUtils.getWallpaperDisplay())

    val wallpaper: StateFlow<WallpaperModel?> = interactor.wallpaperModel
    private val _whichPreview = MutableStateFlow<WhichPreview?>(null)
    private val whichPreview: Flow<WhichPreview> = _whichPreview.asStateFlow().filterNotNull()
    fun setWhichPreview(whichPreview: WhichPreview) {
        _whichPreview.value = whichPreview
    }

    // This is only used for the full screen wallpaper preview.
    private val fullWallpaperPreviewConfigViewModel:
        MutableStateFlow<WallpaperPreviewConfigViewModel?> =
        MutableStateFlow(null)

    // This is only used for the small screen wallpaper preview.
    val smallWallpaper: Flow<Pair<WallpaperModel, WhichPreview>> =
        combine(wallpaper.filterNotNull(), whichPreview) { wallpaper, whichPreview ->
            Pair(wallpaper, whichPreview)
        }

    // This is only used for the full screen wallpaper preview.
    val fullWallpaper: Flow<FullWallpaperPreviewViewModel> =
        combine(
            wallpaper.filterNotNull(),
            fullWallpaperPreviewConfigViewModel.filterNotNull(),
            whichPreview,
        ) { wallpaper, config, whichPreview ->
            FullWallpaperPreviewViewModel(
                wallpaper = wallpaper,
                config = config,
                allowUserCropping =
                    wallpaper is WallpaperModel.StaticWallpaperModel &&
                        !wallpaper.isDownloadableWallpaper(),
                whichPreview = whichPreview,
            )
        }

    // This is only used for the full screen wallpaper preview.
    private val _fullWorkspacePreviewConfigViewModel:
        MutableStateFlow<WorkspacePreviewConfigViewModel?> =
        MutableStateFlow(null)

    // This is only used for the full screen wallpaper preview.
    val fullWorkspacePreviewConfigViewModel: Flow<WorkspacePreviewConfigViewModel> =
        _fullWorkspacePreviewConfigViewModel.filterNotNull()

    val onCropButtonClick: Flow<(() -> Unit)?> =
        combine(wallpaper, fullWallpaperPreviewConfigViewModel.filterNotNull()) {
            wallpaper,
            previewViewModel ->
            if (
                wallpaper is WallpaperModel.StaticWallpaperModel &&
                    !wallpaper.isDownloadableWallpaper()
            ) {
                {
                    staticWallpaperPreviewViewModel.fullPreviewCrop?.let {
                        staticWallpaperPreviewViewModel.updateCropHints(
                            mapOf(previewViewModel.screenOrientation to it)
                        )
                    }
                }
            } else {
                null
            }
        }

    // If the wallpaper is a downloadable wallpaper, do not show the button
    val isSetWallpaperButtonVisible: Flow<Boolean> =
        wallpaper.filterNotNull().map { !it.isDownloadableWallpaper() }

    fun getWorkspacePreviewConfig(
        screen: Screen,
        foldableDisplay: FoldableDisplay?,
    ): WorkspacePreviewConfigViewModel {
        val previewUtils =
            when (screen) {
                Screen.HOME_SCREEN -> {
                    homePreviewUtils
                }
                Screen.LOCK_SCREEN -> {
                    lockPreviewUtils
                }
            }
        val displayId =
            when (foldableDisplay) {
                FoldableDisplay.FOLDED -> {
                    displayUtils.getSmallerDisplay().displayId
                }
                FoldableDisplay.UNFOLDED -> {
                    displayUtils.getWallpaperDisplay().displayId
                }
                null -> {
                    displayUtils.getWallpaperDisplay().displayId
                }
            }
        return WorkspacePreviewConfigViewModel(
            previewUtils = previewUtils,
            displayId = displayId,
        )
    }

    fun onSmallPreviewClicked(
        screen: Screen,
        orientation: ScreenOrientation,
        foldableDisplay: FoldableDisplay?,
    ) {
        fullWallpaperPreviewConfigViewModel.value =
            getWallpaperPreviewConfig(screen, orientation, foldableDisplay)
        _fullWorkspacePreviewConfigViewModel.value =
            getWorkspacePreviewConfig(screen, foldableDisplay)
    }

    fun setDefaultWallpaperPreviewConfigViewModel() {
        fullWallpaperPreviewConfigViewModel.value =
            WallpaperPreviewConfigViewModel(
                Screen.HOME_SCREEN,
                wallpaperDisplaySize,
                ScreenOrientation.PORTRAIT
            )
    }

    private fun getWallpaperPreviewConfig(
        screen: Screen,
        orientation: ScreenOrientation,
        foldableDisplay: FoldableDisplay?,
    ): WallpaperPreviewConfigViewModel {
        val displaySize =
            when (foldableDisplay) {
                FoldableDisplay.FOLDED -> {
                    smallerDisplaySize
                }
                FoldableDisplay.UNFOLDED -> {
                    wallpaperDisplaySize
                }
                null -> {
                    wallpaperDisplaySize
                }
            }
        return WallpaperPreviewConfigViewModel(
            screen = screen,
            displaySize = displaySize,
            screenOrientation = orientation,
        )
    }

    companion object {
        private fun WallpaperModel.isDownloadableWallpaper(): Boolean {
            return this is WallpaperModel.StaticWallpaperModel &&
                this.downloadableWallpaperData != null
        }
    }
}
