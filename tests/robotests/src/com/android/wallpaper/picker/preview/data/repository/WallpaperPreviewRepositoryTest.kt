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

import android.content.Context
import android.content.pm.ActivityInfo
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.android.wallpaper.testing.WallpaperModelUtils.Companion.getStaticWallpaperModel
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import javax.inject.Inject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * Tests for {@link WallpaperPreviewRepository}.
 *
 * WallpaperPreviewRepository cannot be injected in setUp() because it is annotated with scope
 * ActivityRetainedScoped. We make an instance available via TestActivity, which can inject the SUT
 * and expose it for testing.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class WallpaperPreviewRepositoryTest {
    @get:Rule var hiltRule = HiltAndroidRule(this)

    private lateinit var context: Context
    private lateinit var scenario: ActivityScenario<TestActivity>

    @Before
    fun setUp() {
        hiltRule.inject()
        context = ApplicationProvider.getApplicationContext<HiltTestApplication>()

        // Workaround for Studio issues with test-only activities and manifests.
        // See https://github.com/robolectric/robolectric/pull/4736.
        val activityInfo =
            ActivityInfo().apply {
                name = TestActivity::class.java.name
                packageName = context.packageName
            }
        shadowOf(context.packageManager).addOrUpdateActivity(activityInfo)

        scenario = ActivityScenario.launch(TestActivity::class.java)
    }

    @Test
    fun setWallpaperModel() {
        scenario.onActivity {
            val wallpaperModel =
                getStaticWallpaperModel(
                    wallpaperId = "aaa",
                    collectionId = "testCollection",
                )
            val repo = it.repo
            assertThat(repo.wallpaperModel.value).isNull()

            repo.setWallpaperModel(wallpaperModel)

            assertThat(repo.wallpaperModel.value).isEqualTo(wallpaperModel)
        }
    }
}

@AndroidEntryPoint(FragmentActivity::class)
class TestActivity @Inject constructor() : Hilt_TestActivity() {
    @Inject lateinit var repo: WallpaperPreviewRepository
}
