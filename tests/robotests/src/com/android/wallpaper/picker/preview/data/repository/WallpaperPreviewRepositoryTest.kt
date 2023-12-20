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

package com.android.wallpaper.picker.preview.data.repository

import android.app.Activity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.android.wallpaper.model.wallpaper.WallpaperModel
import com.android.wallpaper.picker.preview.data.util.LiveWallpaperDownloader
import com.android.wallpaper.picker.preview.shared.model.LiveWallpaperDownloadResultCode
import com.android.wallpaper.picker.preview.shared.model.LiveWallpaperDownloadResultModel
import com.android.wallpaper.testing.WallpaperModelUtils.Companion.getStaticWallpaperModel
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests for {@link WallpaperPreviewRepository}.
 *
 * WallpaperPreviewRepository cannot be injected in setUp() because it is annotated with scope
 * ActivityRetainedScoped. We make an instance available via TestActivity, which can inject the SUT
 * and expose it for testing.
 */
@RunWith(RobolectricTestRunner::class)
class WallpaperPreviewRepositoryTest {
    private val testDownloader = TestLiveWallpaperDownloader()

    private lateinit var testScope: TestScope
    private lateinit var repo: WallpaperPreviewRepository

    @Before
    fun setUp() {
        val testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)
        repo = WallpaperPreviewRepository(testDownloader, testDispatcher)
    }

    @Test
    fun setWallpaperModel() {
        val wallpaperModel =
            getStaticWallpaperModel(
                wallpaperId = "aaa",
                collectionId = "testCollection",
            )
        assertThat(repo.wallpaperModel.value).isNull()

        repo.setWallpaperModel(wallpaperModel)

        assertThat(repo.wallpaperModel.value).isEqualTo(wallpaperModel)
    }

    @Test
    fun downloadWallpaper_fails() =
        testScope.runTest {
            val result = repo.downloadWallpaper()

            assertThat(result).isNull()
        }

    @Test
    fun downloadWallpaper_succeeds() =
        testScope.runTest {
            testDownloader.setDownloadResult(
                LiveWallpaperDownloadResultModel(LiveWallpaperDownloadResultCode.SUCCESS, null)
            )

            val result = repo.downloadWallpaper()

            assertThat(result).isNotNull()
            assertThat(result!!.wallpaperModel).isNull()
        }
}

class TestLiveWallpaperDownloader : LiveWallpaperDownloader {
    private var downloadResult: LiveWallpaperDownloadResultModel? = null

    override fun initiateDownloadableService(
        activity: Activity,
        wallpaperData: WallpaperModel.StaticWallpaperModel,
        intentSenderLauncher: ActivityResultLauncher<IntentSenderRequest>
    ) {}

    override fun cleanup() {}

    fun setDownloadResult(result: LiveWallpaperDownloadResultModel) {
        downloadResult = result
    }

    override suspend fun downloadWallpaper(): LiveWallpaperDownloadResultModel? {
        return downloadResult
    }
}
