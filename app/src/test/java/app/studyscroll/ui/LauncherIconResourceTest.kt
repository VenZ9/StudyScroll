package app.studyscroll.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LauncherIconResourceTest {

    private val resDir: File = run {
        val direct = File("src/main/res")
        if (direct.exists()) direct else File("app/src/main/res")
    }

    @Test
    fun `adaptive icon resources exist and foreground icon conforms to standard dimensions`() {
        val foreground = File(resDir, "drawable/ic_launcher_foreground.xml")
        val background = File(resDir, "drawable/ic_launcher_background.xml")
        val legacy = File(resDir, "drawable/ic_launcher.xml")
        val legacyRound = File(resDir, "drawable/ic_launcher_round.xml")
        val mipmapBase = File(resDir, "mipmap/ic_launcher.xml")
        val mipmapBaseRound = File(resDir, "mipmap/ic_launcher_round.xml")
        val adaptiveXml = File(resDir, "mipmap-anydpi-v26/ic_launcher.xml")
        val adaptiveRoundXml = File(resDir, "mipmap-anydpi-v26/ic_launcher_round.xml")

        assertTrue("Foreground drawable must exist", foreground.exists())
        assertTrue("Background drawable must exist", background.exists())
        assertTrue("Legacy launcher drawable must exist", legacy.exists())
        assertTrue("Legacy round launcher drawable must exist", legacyRound.exists())
        assertTrue("Base mipmap launcher drawable must exist", mipmapBase.exists())
        assertTrue("Base mipmap round launcher drawable must exist", mipmapBaseRound.exists())
        assertTrue("Adaptive icon XML must exist", adaptiveXml.exists())
        assertTrue("Adaptive round icon XML must exist", adaptiveRoundXml.exists())

        // Verify foreground content
        val foregroundContent = foreground.readText()
        assertTrue("Foreground must use 108dp viewport", foregroundContent.contains("android:viewportWidth=\"108\""))
        assertTrue("Foreground must use 108dp viewport", foregroundContent.contains("android:viewportHeight=\"108\""))
        assertFalse("Foreground must not use glitched 0.55 scale", foregroundContent.contains("scaleX=\"0.55\""))
        assertFalse("Foreground must not use glitched 0.55 scale", foregroundContent.contains("scaleY=\"0.55\""))

        // Verify adaptive icon points to proper foreground and background
        val adaptiveContent = adaptiveXml.readText()
        assertTrue("Adaptive icon must reference ic_launcher_background", adaptiveContent.contains("@drawable/ic_launcher_background"))
        assertTrue("Adaptive icon must reference ic_launcher_foreground", adaptiveContent.contains("@drawable/ic_launcher_foreground"))
    }
}
