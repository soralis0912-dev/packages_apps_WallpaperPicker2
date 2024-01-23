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

package com.android.wallpaper.picker.preview.data.repository

import android.content.Context
import android.net.Uri
import com.android.wallpaper.effects.Effect
import com.android.wallpaper.effects.EffectContract
import com.android.wallpaper.effects.EffectsController
import com.android.wallpaper.picker.di.modules.BackgroundDispatcher
import com.android.wallpaper.widget.floatingsheetcontent.WallpaperEffectsView2.EffectTextRes
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

@Singleton
class EffectsRepository
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val effectsController: EffectsController,
    @BackgroundDispatcher private val bgDispatcher: CoroutineDispatcher,
) {
    enum class EffectStatus {
        EFFECT_DISABLE,
        EFFECT_READY,
        EFFECT_DOWNLOAD_READY,
        EFFECT_DOWNLOAD_IN_PROGRESS,
        EFFECT_APPLY_IN_PROGRESS,
        EFFECT_APPLIED,
    }

    private val _effectStatus = MutableStateFlow(EffectStatus.EFFECT_DISABLE)
    val effectStatus = _effectStatus.asStateFlow()
    val wallpaperEffect = MutableStateFlow<Effect?>(null)

    suspend fun initializeEffect() {
        withContext(bgDispatcher) {
            val listener =
                EffectsController.EffectsServiceListener {
                    _,
                    bundle,
                    resultCode,
                    originalStatusCode,
                    errorMessage ->
                    when (resultCode) {
                        EffectsController.RESULT_PROBE_SUCCESS -> {
                            _effectStatus.value = EffectStatus.EFFECT_READY
                        }
                        EffectsController.RESULT_ERROR_PROBE_SUPPORT_FOREGROUND -> {
                            // TODO also check the flag InjectorProvider.getInjector().getFlags()
                            //      isWallpaperEffectModelDownloadEnabled()
                            _effectStatus.value = EffectStatus.EFFECT_DOWNLOAD_READY
                        }
                        EffectsController.RESULT_PROBE_FOREGROUND_DOWNLOADING -> {
                            _effectStatus.value = EffectStatus.EFFECT_DOWNLOAD_IN_PROGRESS
                        }
                        EffectsController.RESULT_FOREGROUND_DOWNLOAD_SUCCEEDED -> {
                            // TODO Maybe we should check isEffectTriggered
                            _effectStatus.value = EffectStatus.EFFECT_READY
                        }
                        EffectsController.RESULT_FOREGROUND_DOWNLOAD_FAILED -> {
                            _effectStatus.value = EffectStatus.EFFECT_DOWNLOAD_READY
                        }
                        EffectsController.RESULT_SUCCESS -> {
                            _effectStatus.value = EffectStatus.EFFECT_APPLIED
                        }
                        EffectsController.RESULT_SUCCESS_WITH_GENERATION_ERROR -> {
                            _effectStatus.value = EffectStatus.EFFECT_APPLIED
                        }
                        EffectsController.RESULT_SUCCESS_REUSED -> {
                            _effectStatus.value = EffectStatus.EFFECT_APPLIED
                        }
                        else -> {
                            // TODO onImageEffectFailed
                            _effectStatus.value = EffectStatus.EFFECT_READY
                        }
                    }
                }
            // TODO remove listener when destroy
            effectsController.setListener(listener)

            effectsController.contentUri.let { uri ->
                if (Uri.EMPTY.equals(uri)) {
                    return@withContext
                }

                // Query effect provider
                context.contentResolver
                    .query(
                        uri,
                        /* projection= */ null,
                        /* selection= */ null,
                        /* selectionArgs= */ null,
                        /* sortOrder= */ null
                    )
                    ?.use {
                        while (it.moveToNext()) {
                            val titleRow: Int = it.getColumnIndex(EffectContract.KEY_EFFECT_TITLE)
                            val idRow: Int = it.getColumnIndex(EffectContract.KEY_EFFECT_ID)
                            wallpaperEffect.value =
                                Effect(
                                    it.getInt(idRow),
                                    it.getString(titleRow),
                                    effectsController.targetEffect
                                )
                        }
                    }
            }

            if (effectsController.isEffectTriggered) {
                _effectStatus.value = EffectStatus.EFFECT_READY
            } else {
                effectsController.triggerEffect(context)
            }
        }
    }

    fun enableImageEffect(effect: EffectsController.EffectEnumInterface) {
        // TODO implement enabling effect
        _effectStatus.value = EffectStatus.EFFECT_APPLY_IN_PROGRESS
    }

    fun disableImageEffect() {
        // TODO implement disabling effect
    }

    fun destroy() {
        effectsController.removeListener()
    }

    fun isTargetEffect(effect: EffectsController.EffectEnumInterface): Boolean {
        return effectsController.isTargetEffect(effect)
    }

    fun getEffectTextRes(): EffectTextRes {
        return EffectTextRes(
            effectsController.effectTitle,
            effectsController.effectFailedTitle,
            effectsController.effectSubTitle,
            effectsController.retryInstruction,
            effectsController.noEffectInstruction,
        )
    }
}
