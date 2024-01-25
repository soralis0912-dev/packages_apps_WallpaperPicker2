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

import android.stats.style.StyleEnums
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.wallpaper.model.wallpaper.FoldableDisplay
import com.android.wallpaper.model.wallpaper.ScreenOrientation
import com.android.wallpaper.module.CustomizationSections.Screen
import com.android.wallpaper.picker.customization.shared.model.WallpaperColorsModel
import com.android.wallpaper.picker.customization.shared.model.WallpaperDestination
import com.android.wallpaper.picker.data.WallpaperModel
import com.android.wallpaper.picker.data.WallpaperModel.LiveWallpaperModel
import com.android.wallpaper.picker.data.WallpaperModel.StaticWallpaperModel
import com.android.wallpaper.picker.di.modules.PreviewUtilsModule.HomeScreenPreviewUtils
import com.android.wallpaper.picker.di.modules.PreviewUtilsModule.LockScreenPreviewUtils
import com.android.wallpaper.picker.preview.domain.interactor.WallpaperPreviewInteractor
import com.android.wallpaper.picker.preview.ui.WallpaperPreviewActivity
import com.android.wallpaper.util.DisplayUtils
import com.android.wallpaper.util.PreviewUtils
import com.android.wallpaper.util.WallpaperConnection.WhichPreview
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

/** Top level [ViewModel] for [WallpaperPreviewActivity] and its fragments */
@HiltViewModel
class WallpaperPreviewViewModel
@Inject
constructor(
    private val interactor: WallpaperPreviewInteractor,
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

    private val _wallpaperConnectionColors: MutableStateFlow<WallpaperColorsModel> =
        MutableStateFlow(WallpaperColorsModel.Loading as WallpaperColorsModel).apply {
            viewModelScope.launch {
                delay(1000)
                if (value == WallpaperColorsModel.Loading) {
                    emit(WallpaperColorsModel.Loaded(null))
                }
            }
        }
    private val liveWallpaperColors: Flow<WallpaperColorsModel> =
        wallpaper
            .filter { it is LiveWallpaperModel }
            .combine(_wallpaperConnectionColors) { _, wallpaperConnectionColors ->
                wallpaperConnectionColors
            }
    val wallpaperColorsModel: Flow<WallpaperColorsModel> =
        merge(liveWallpaperColors, staticWallpaperPreviewViewModel.wallpaperColors)

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
                    wallpaper is StaticWallpaperModel && !wallpaper.isDownloadableWallpaper(),
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
            if (wallpaper is StaticWallpaperModel && !wallpaper.isDownloadableWallpaper()) {
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

    private val _isSetWallpaperProgressBarVisible = MutableStateFlow(false)
    val isSetWallpaperProgressBarVisible: Flow<Boolean> =
        _isSetWallpaperProgressBarVisible.asStateFlow()

    val onSetWallpaperButtonClicked: Flow<(() -> Unit)?> =
        combine(
            wallpaper,
            staticWallpaperPreviewViewModel.fullResWallpaperViewModel,
        ) { wallpaper, fullResWallpaperViewModel ->
            if (
                wallpaper == null ||
                    wallpaper.isDownloadableWallpaper() ||
                    (wallpaper is StaticWallpaperModel && fullResWallpaperViewModel == null)
            ) {
                null
            } else {
                { showSetWallpaperDialog(wallpaper, fullResWallpaperViewModel) }
            }
        }
    val isSetWallpaperButtonVisible: Flow<Boolean> = onSetWallpaperButtonClicked.map { it != null }

    private val _setWallpaperDialog = MutableStateFlow<WallpaperConfirmDialogViewModel?>(null)
    val setWallpaperDialog = _setWallpaperDialog.asStateFlow()

    private fun showSetWallpaperDialog(
        viewModel: WallpaperModel,
        scaleImageViewModel: FullResWallpaperViewModel?,
    ) {
        _setWallpaperDialog.value =
            WallpaperConfirmDialogViewModel(
                onConfirmButtonClicked = {
                    _isSetWallpaperProgressBarVisible.value = true
                    _setWallpaperDialog.value = null
                    when (viewModel) {
                        is StaticWallpaperModel ->
                            scaleImageViewModel?.let {
                                interactor.setStaticWallpaper(
                                    setWallpaperEntryPoint =
                                        StyleEnums.SET_WALLPAPER_ENTRY_POINT_WALLPAPER_PREVIEW,
                                    destination = WallpaperDestination.BOTH,
                                    wallpaperModel = viewModel,
                                    inputStream = it.stream,
                                    bitmap = it.rawWallpaperBitmap,
                                    cropHints = it.cropHints ?: emptyMap(),
                                )
                            }
                        is LiveWallpaperModel -> {
                            interactor.setLiveWallpaper(
                                setWallpaperEntryPoint =
                                    StyleEnums.SET_WALLPAPER_ENTRY_POINT_WALLPAPER_PREVIEW,
                                destination = WallpaperDestination.BOTH,
                                wallpaperModel = viewModel,
                            )
                        }
                    }
                },
                onCancelButtonClicked = { _setWallpaperDialog.value = null },
            )
    }

    fun dismissSetWallpaperDialog() {
        _setWallpaperDialog.value = null
    }

    fun setWallpaperConnectionColors(wallpaperColors: WallpaperColorsModel) {
        _wallpaperConnectionColors.value = wallpaperColors
    }

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
            return this is StaticWallpaperModel && this.downloadableWallpaperData != null
        }
    }
}
