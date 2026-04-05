package com.beackers.dumbhome

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.beackers.dumbhome.launcher.LauncherActivity

class SettingsActivity : AppCompatActivity() {
    private lateinit var prefs: Prefs
    private var currentPrefKey: String? = null
    private lateinit var list: RecyclerView
    private lateinit var adapter: SimpleTextAdapter
    private val rows = mutableListOf<String>()

    private val cropWallpaper = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        refreshRows()
    }

    private val pickWallpaper = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult
        cropWallpaper.launch(
            Intent(this, WallpaperCropActivity::class.java)
                .putExtra(WallpaperCropActivity.EXTRA_IMAGE_URI, uri),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefs = Prefs(this)
        list = findViewById(R.id.settingsList)
        list.layoutManager = LinearLayoutManager(this)
        adapter = SimpleTextAdapter(rows) { onClickRow(it) }
        list.adapter = adapter

        refreshRows()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 100 && resultCode == RESULT_OK) {
          val packageName = data?.getStringExtra("package") ?: return
          val key = currentPrefKey ?: return
          prefs.setShortcut(key, ShortcutAction.OPEN_ACTIVITY)
          prefs.setShortcutApp(key, packageName)
          refreshRows()
      }
    }

    private fun refreshRows() {
        rows.clear()
        rows += "Change home image"
        rows += "Configure F11 (${prefs.getShortcut(Prefs.KEY_F11).displayName})"
        rows += "Configure Menu (${prefs.getShortcut(Prefs.KEY_MENU).displayName})"
        rows += "Configure Up (${prefs.getShortcut(Prefs.KEY_UP).displayName})"
        rows += "Configure Down (${prefs.getShortcut(Prefs.KEY_DOWN).displayName})"
        rows += "Configure Left (${prefs.getShortcut(Prefs.KEY_LEFT).displayName})"
        rows += "Configure Right (${prefs.getShortcut(Prefs.KEY_RIGHT).displayName})"
        rows += "Close"
        adapter.submit(rows)
    }

    private fun onClickRow(position: Int) {
        when (position) {
            0 -> pickWallpaper.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            1 -> pickAction(Prefs.KEY_F11)
            2 -> pickAction(Prefs.KEY_MENU)
            3 -> pickAction(Prefs.KEY_UP)
            4 -> pickAction(Prefs.KEY_DOWN)
            5 -> pickAction(Prefs.KEY_LEFT)
            6 -> pickAction(Prefs.KEY_RIGHT)
            7 -> finish()
        }
    }

    private fun pickAction(prefKey: String) {
        val actions = ShortcutAction.entries
        AlertDialog.Builder(this)
            .setTitle("Shortcut action")
            .setItems(actions.map { it.displayName }.toTypedArray()) { _, which ->
                if (actions[which] == ShortcutAction.OPEN_ACTIVITY) {
                    currentPrefKey = prefKey
                    pickApp(prefKey)
                } else {
                    prefs.setShortcut(prefKey, actions[which])
                    refreshRows()
                }
            }
            .show()
    }

    private fun pickApp(prefKey: String) {
        val intent = Intent(this, LauncherActivity::class.java)
        intent.putExtra("pick_mode", true)
        startActivityForResult(intent, 100)
    }
}
