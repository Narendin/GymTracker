package com.gymtracker.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

/**
 * Передача скачанного APK системному установщику.
 *
 * Файл лежит в приватном каталоге приложения и подаётся через FileProvider:
 * с Android 10 установщик не читает чужие файлы напрямую. Установка требует
 * разрешения «Установка из неизвестных источников» для этого приложения.
 */
class ApkInstaller(private val context: Context) {

    /** Есть ли разрешение на установку из приложения. */
    fun canInstall(): Boolean =
        runCatching { context.packageManager.canRequestPackageInstalls() }.getOrDefault(false)

    /** Экран системных настроек с разрешением для этого приложения. */
    fun openPermissionSettings(): Boolean = runCatching {
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        true
    }.getOrDefault(false)

    /**
     * Запустить системный установщик для файла. Возвращает true, если
     * намерение принято.
     */
    fun install(file: File): Boolean = runCatching {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = uri
            type = MIME_APK
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        true
    }.getOrDefault(false)

    companion object {
        private const val MIME_APK = "application/vnd.android.package-archive"
    }
}
