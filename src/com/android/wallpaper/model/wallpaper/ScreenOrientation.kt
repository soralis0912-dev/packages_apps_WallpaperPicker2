/*
 * Copyright 2023 The Android Open Source Project
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
package com.android.wallpaper.model.wallpaper

import android.graphics.Point

/** Orientation of the screen. */
enum class ScreenOrientation {
    /** For screen of handheld, tablet, and outer screen of foldable, in portrait mode */
    PORTRAIT,
    /** For screen of handheld, tablet, and outer screen of foldable, in landscape mode */
    LANDSCAPE,
    /** For inner screen of foldable, in portrait mode */
    SQUARE_PORTRAIT,
    /** For inner screen of foldable, in landscape mode */
    SQUARE_LANDSCAPE,
}

private val orientationToSize = mutableMapOf<ScreenOrientation, Point>()

fun getDisplaySize(orientation: ScreenOrientation): Point =
    checkNotNull(orientationToSize[orientation])

/**
 * Gets the [ScreenOrientation] based on the display and its size.
 *
 * @param displaySize size of the display to get screen orientation for.
 * @param wallpaperDisplaySize size of the internal display with the largest area.
 * @param foldableDisplay the display of [displaySize] on foldable devices, null for single display
 *   devices.
 */
// TODO (b/303317694): remove screen orientation, it is no longer needed to set multi crop wallpaper
fun getScreenOrientation(
    displaySize: Point,
    wallpaperDisplaySize: Point,
    foldableDisplay: FoldableDisplay? = null,
): ScreenOrientation {
    val orientation =
        if (foldableDisplay == null) {
            if (displaySize.y >= displaySize.x) ScreenOrientation.PORTRAIT
            else ScreenOrientation.LANDSCAPE
        } else if (foldableDisplay == FoldableDisplay.FOLDED) {
            if (displaySize == wallpaperDisplaySize) {
                ScreenOrientation.SQUARE_PORTRAIT
            } else {
                ScreenOrientation.PORTRAIT
            }
        } else {
            if (displaySize == wallpaperDisplaySize) {
                if (displaySize.y >= displaySize.x) ScreenOrientation.SQUARE_LANDSCAPE
                else ScreenOrientation.SQUARE_PORTRAIT
            } else {
                ScreenOrientation.PORTRAIT
            }
        }
    orientationToSize[orientation] = displaySize

    return orientation
}
