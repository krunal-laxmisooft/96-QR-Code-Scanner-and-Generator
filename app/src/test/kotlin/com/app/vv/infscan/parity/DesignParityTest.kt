package com.app.vv.infscan.parity

import android.app.Application
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
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
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Renders every designed screen at 360x800 dp so it can be compared with /design-reference. */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "en-rUS-w360dp-h800dp-xhdpi")
class DesignParityTest(private val sceneName: String) {

    @get:Rule val compose = createComposeRule()

    @Before
    fun disableAnimations() {
        val resolver = ApplicationProvider.getApplicationContext<Application>().contentResolver
        Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
    }

    @Test
    fun render() {
        val scene = scenes.first { it.name == sceneName }
        compose.setContent {
            InfScanTheme(darkTheme = scene.dark) {
                Box(Modifier.size(360.dp, 800.dp)) { scene.content() }
            }
        }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/${scene.name}.png")
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun scenes(): List<Array<Any>> = com.app.vv.infscan.parity.scenes.map { arrayOf(it.name) }
    }
}
