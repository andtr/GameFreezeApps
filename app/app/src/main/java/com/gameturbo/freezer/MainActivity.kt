package com.gameturbo.freezer

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var gameSelect: Spinner
    private lateinit var appsLayout: LinearLayout
    private val packagesList = mutableListOf<Pair<String, String>>()
    private val selectedApps = mutableSetOf<String>()
    private var selectedGame = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }
        scroll.addView(root)

        val title = TextView(this).apply {
            text = "🎮 Game Freezer Control"
            textSize = 22f
            setPadding(0, 0, 0, 40)
        }
        root.addView(title)

        val gameLabel = TextView(this).apply { text = "1. Selecciona el juego:" }
        root.addView(gameLabel)

        gameSelect = Spinner(this)
        root.addView(gameSelect)

        val appsLabel = TextView(this).apply { 
            text = "2. Apps a congelar:"
            setPadding(0, 40, 0, 20)
        }
        root.addView(appsLabel)

        appsLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(appsLayout)

        val saveBtn = Button(this).apply {
            text = "💾 Guardar y Aplicar al Módulo"
            setPadding(0, 40, 0, 40)
            setOnClickListener { saveConfig() }
        }
        root.addView(saveBtn)

        setContentView(scroll)

        loadApps()
    }

    private fun loadApps() {
        val pm = packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        
        val gameNames = mutableListOf<String>()
        val gamePkgs = mutableListOf<String>()

        for (app in apps) {
            val label = pm.getApplicationLabel(app).toString()
            packagesList.add(Pair(app.packageName, label))
        }
        
        packagesList.sortBy { it.second }

        for (app in packagesList) {
            gameNames.add(app.second)
            gamePkgs.add(app.first)
            
            val checkBox = CheckBox(this).apply {
                text = app.second
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) selectedApps.add(app.first)
                    else selectedApps.remove(app.first)
                }
            }
            appsLayout.addView(checkBox)
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, gameNames)
        gameSelect.adapter = adapter

        gameSelect.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                selectedGame = gamePkgs[position]
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun saveConfig() {
        if (selectedGame.isEmpty() || selectedApps.isEmpty()) {
            Toast.makeText(this, "Selecciona un juego y al menos una app", Toast.LENGTH_SHORT).show()
            return
        }

        val configText = StringBuilder()
        configText.append("juego=$selectedGame\n")
        for (pkg in selectedApps) {
            configText.append("freeze=$pkg\n")
        }

        try {
            val tempFile = File(cacheDir, "config.txt")
            tempFile.writeText(configText.toString())
            
            val command = "cp ${tempFile.absolutePath} /data/adb/modules/game-freezer/config.txt && chmod 644 /data/adb/modules/game-freezer/config.txt"
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            process.waitFor()

            if (process.exitValue() == 0) {
                Toast.makeText(this, "✅ ¡Guardado en el módulo con éxito!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "❌ Error de Root. ¿APatch dio permisos?", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "❌ Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
