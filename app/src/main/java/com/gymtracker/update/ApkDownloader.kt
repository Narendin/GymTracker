package com.gymtracker.update

import java.net.HttpURLConnection
import java.net.URL
import java.io.File

/**
 * Скачивание APK во внутренний каталог приложения.
 *
 * Каталог — приватный кэш (`cacheDir/updates`): он объявлен в путях
 * FileProvider и подаётся системному установщику. Перед скачиванием старые
 * файлы удаляются, чтобы на телефоне не накапливались прошлые версии.
 */
class ApkDownloader(private val updatesDir: File) {

    /**
     * Скачать APK по ссылке; возвращает файл или null, если скачивание
     * не удалось (сеть, обрыв, пустой ответ).
     */
    fun download(release: Release): File? = runCatching {
        updatesDir.mkdirs()
        cleanOldFiles()
        val connection = URL(release.downloadUrl).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.connect()
            val code = connection.responseCode
            if (code !in 200..299) return null
            val target = File(updatesDir, fileName(release.tag))
            connection.inputStream.use { input -> target.outputStream().use { output -> input.copyTo(output) } }
            // Пустой или крошечный файл — обрыв соединения, не APK.
            if (target.length() < MIN_APK_BYTES) {
                target.delete()
                return null
            }
            target
        } finally {
            // Соединение закрываем и при обрыве: иначе оно висит до сборки мусора.
            connection.disconnect()
        }
    }.getOrNull()

    /** Удалить ранее скачанные APK: они больше не нужны. */
    private fun cleanOldFiles() {
        updatesDir.listFiles()?.filter { it.extension == "apk" }?.forEach { it.delete() }
    }

    /**
     * Имя файла по номеру релиза. Символы кроме букв, цифр, точек и дефиса
     * убираются: номер из ответа сети не должен превратиться в путь.
     */
    private fun fileName(tag: String): String {
        val safe = tag.filter { it.isLetterOrDigit() || it == '.' || it == '-' }
        return "update-$safe.apk"
    }

    companion object {
        private const val TIMEOUT_MS = 15_000
        // APK заведомо больше мегабайта; меньший размер означает обрыв.
        private const val MIN_APK_BYTES = 1_000_000L
    }
}
