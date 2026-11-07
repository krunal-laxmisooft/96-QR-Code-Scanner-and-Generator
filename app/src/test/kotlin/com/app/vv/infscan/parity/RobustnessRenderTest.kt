package com.app.vv.infscan.parity

import android.app.Application
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.app.vv.infscan.ui.theme.InfScanTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Renders key screens at 200% font scale and in a right-to-left layout so clipping shows up in review. */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "en-rUS-w360dp-h800dp-xhdpi")
class RobustnessRenderTest(private val sceneName: String, private val mode: String) {

    @get:Rule val compose = createComposeRule()

    @Before
    fun disableAnimations() {
        val resolver = ApplicationProvider.getApplicationContext<Application>().contentResolver
        Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
    }

    @Test
    fun render() {
        if (mode == LARGE_FONT) RuntimeEnvironment.setFontScale(2f)
        val scene = scenes.first { it.name == sceneName }
        compose.setContent {
            InfScanTheme(darkTheme = scene.dark) {
                Box(Modifier.size(360.dp, 800.dp)) { scene.content() }
            }
        }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/robust/$sceneName-$mode.png")
    }

    companion object {
        private const val LARGE_FONT = "font200"
        private val names = listOf("S02-Onboarding-1", "S06-Scan-Camera-Denied", "S08-Result-Link", "S09-Result-Product", "S11-Create", "S12-Create-WiFi", "S14-History", "S16-Settings")

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0} {1}")
        fun cases(): List<Array<Any>> = names.map { arrayOf<Any>(it, LARGE_FONT) }
    }
}
