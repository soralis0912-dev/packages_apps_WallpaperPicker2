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

package com.android.wallpaper.picker.preview.domain.interactor

import android.graphics.Bitmap
import android.graphics.Rect
import com.android.wallpaper.model.wallpaper.ScreenOrientation
import com.android.wallpaper.model.wallpaper.WallpaperModel
import com.android.wallpaper.model.wallpaper.WallpaperModel.StaticWallpaperModel
import com.android.wallpaper.module.logging.UserEventLogger
import com.android.wallpaper.picker.customization.data.repository.WallpaperRepository
import com.android.wallpaper.picker.customization.shared.model.WallpaperDestination
import com.android.wallpaper.picker.preview.data.repository.WallpaperPreviewRepository
import dagger.hilt.android.scopes.ActivityRetainedScoped
import java.io.InputStream
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@ActivityRetainedScoped
class WallpaperPreviewInteractor
@Inject
constructor(
    wallpaperPreviewRepository: WallpaperPreviewRepository,
    private val wallpaperRepository: WallpaperRepository,
) {
    val wallpaperModel: StateFlow<WallpaperModel?> = wallpaperPreviewRepository.wallpaperModel

    suspend fun setStaticWallpaper(
        @UserEventLogger.SetWallpaperEntryPoint setWallpaperEntryPoint: Int,
        destination: WallpaperDestination,
        wallpaperModel: StaticWallpaperModel,
        inputStream: InputStream?,
        bitmap: Bitmap,
        cropHints: Map<ScreenOrientation, Rect>,
        onDone: () -> Unit,
    ) {
        wallpaperRepository.setStaticWallpaper(
            setWallpaperEntryPoint,
            destination,
            wallpaperModel,
            inputStream,
            bitmap,
            cropHints,
            onDone,
        )
    }
}
