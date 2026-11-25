package com.gymtracker.update

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Последняя запись релизов: номер, дата, описание релиза, ссылка на APK. */
data class Release(
    val tag: String,
    val publishedAt: String,
    val notes: String,
    val downloadUrl: String,
)

/** Что приложение узнало о последней версии. */
sealed class UpdateResult {
    /** Приложение уже последней версии. */
    object UpToDate : UpdateResult()

    /** Есть версия новее текущей. */
    data class NewVersion(val release: Release) : UpdateResult()

    /** Сеть недоступна: запрос не прошёл. */
    object NetworkError : UpdateResult()

    /** Ответ пришёл, но разобрать номер и ссылку не удалось. */
    object UnreadableResponse : UpdateResult()
}

/**
 * Проверка наличия новой версии.
 *
 * Интерфейс, а не конкретный клиент: результат подменяется в автотестах,
 * где сеть недоступна и отвечать должен фиксированный сценарий.
 */
interface UpdateChecker {
    suspend fun check(currentVersion: String): UpdateResult
}

/**
 * Единственное сетевое обращение приложения: один запрос по нажатию кнопки.
 * Репозиторий публичный, поэтому доступ без токена; данных журнала запрос не
 * касается.
 */
class GithubUpdateChecker : UpdateChecker {

    override suspend fun check(currentVersion: String): UpdateResult = withContext(Dispatchers.IO) {
        val text = fetchJson() ?: return@withContext UpdateResult.NetworkError
        val release = parseRelease(text) ?: return@withContext UpdateResult.UnreadableResponse
        when (compareVersions(currentVersion, release.tag)) {
            null -> UpdateResult.UnreadableResponse
            1 -> UpdateResult.NewVersion(release)
            else -> UpdateResult.UpToDate
        }
    }

    /** Текст ответа релизной стороны; null — сеть недоступна или ответ нечитаем. */
    private fun fetchJson(): String? = runCatching {
        val connection = URL(API_URL).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.connect()
            val code = connection.responseCode
            if (code in 200..299) connection.inputStream.bufferedReader().use { it.readText() } else null
        } finally {
            // Соединение закрываем и при обрыве: иначе оно висит до сборки мусора.
            connection.disconnect()
        }
    }.getOrNull()

    /** Разбор ответа: номер, дата, описание и ссылка на APK первого вложения. */
    private fun parseRelease(text: String): Release? = runCatching {
        val json = JSONObject(text)
        val tag = json.optString("tag_name").ifBlank { return null }
        val assets = json.optJSONArray("assets") ?: return null
        var url = ""
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            val link = asset.optString("browser_download_url")
            if (link.endsWith(".apk")) {
                url = link
                break
            }
        }
        if (url.isBlank()) return null
        Release(
            tag = tag,
            publishedAt = json.optString("published_at"),
            notes = json.optString("body"),
            downloadUrl = url,
        )
    }.getOrNull()

    companion object {
        private const val API_URL = "https://api.github.com/repos/Narendin/GymTracker/releases/latest"
        private const val TIMEOUT_MS = 10_000
    }
}
