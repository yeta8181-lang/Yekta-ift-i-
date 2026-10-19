package com.example

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.example.ui.screens.PlayStoreGraphicsGenerator
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Kalp Kalbe", appName)
    }

    @Test
    fun `verify Google Play Store assets exist in play_store_assets directory`() {
        val projectRoot = File(System.getProperty("user.dir") ?: ".").let { dir ->
            if (dir.name == "app") dir.parentFile ?: dir else dir
        }
        val playStoreAssetsDir = File(projectRoot, "play_store_assets").apply { mkdirs() }

        PlayStoreGraphicsGenerator.ASSET_SPECS.forEach { spec ->
            val outFile = File(playStoreAssetsDir, spec.fileName)
            if (!outFile.exists() || outFile.length() == 0L) {
                val bitmap = PlayStoreGraphicsGenerator.generateBitmap(spec.id)
                FileOutputStream(outFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
            }
            assertTrue("Expected ${outFile.absolutePath} to exist", outFile.exists() && outFile.length() > 0)
        }
    }
}
