package com.gymtracker.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymtracker.BuildConfig
import com.gymtracker.settings.SettingsStore
import com.gymtracker.settings.ThemeMode
import com.gymtracker.ui.components.ScreenTitle
import com.gymtracker.ui.theme.LocalGymPalette

/** D1. Настройки: тема, серые пункты фазы 2, о приложении. */
@Composable
fun SettingsScreen(settings: SettingsStore) {
    val palette = LocalGymPalette.current
    val mode = settings.themeMode.collectAsState()

    Column(Modifier.fillMaxWidth()) {
        ScreenTitle("Настройки")
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        ) {
            SectionLabel("Тема")
            ThemeOption("Тёмная", ThemeMode.DARK, mode.value, settings)
            ThemeOption("Светлая", ThemeMode.LIGHT, mode.value, settings)
            ThemeOption("Как в системе", ThemeMode.SYSTEM, mode.value, settings)

            SectionLabel("Данные")
            SettingsRow("Экспорт", "фаза 2", enabled = false)
            SettingsRow("Импорт архива", "фаза 2", enabled = false)
            SettingsRow("Проверка обновлений", "фаза 2", enabled = false)

            SectionLabel("О приложении")
            SettingsRow("Версия", BuildConfig.VERSION_NAME, enabled = true)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    val palette = LocalGymPalette.current
    Text(
        text,
        fontSize = 13.sp,
        color = palette.muted,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
    )
}

@Composable
private fun ThemeOption(label: String, mode: ThemeMode, current: ThemeMode, settings: SettingsStore) {
    val palette = LocalGymPalette.current
    val selected = mode == current
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(palette.panel)
            .clickable { settings.setThemeMode(mode) }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 17.sp, color = palette.text)
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(if (selected) palette.accent else palette.panel2),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(palette.onAccent))
            }
        }
    }
}

@Composable
private fun SettingsRow(title: String, value: String, enabled: Boolean) {
    val palette = LocalGymPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(palette.panel)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            fontSize = 17.sp,
            color = if (enabled) palette.text else palette.muted,
        )
        Spacer(Modifier.weight(1f))
        Text(
            value,
            fontSize = 13.sp,
            fontWeight = if (enabled) FontWeight.SemiBold else FontWeight.Normal,
            color = if (enabled) palette.accent else palette.muted,
        )
    }
}
