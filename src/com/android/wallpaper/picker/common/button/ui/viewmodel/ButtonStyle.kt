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
 *
 */

package com.android.wallpaper.picker.common.button.ui.viewmodel

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StyleRes
import com.android.wallpaper.R

sealed class ButtonStyle(
    @ColorRes open val textColorRes: Int,
    @DrawableRes open val backgroundDrawableRes: Int,
) {
    data object Primary :
        ButtonStyle(
            textColorRes = R.color.system_on_primary,
            backgroundDrawableRes = R.drawable.primary_dialog_button_background,
        )
    data object Secondary :
        ButtonStyle(
            textColorRes = R.color.system_on_surface,
            backgroundDrawableRes = R.drawable.secondary_dialog_button_background,
        )
}
