package com.gymtracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.gymtracker.ui.theme.LocalGymPalette
import java.io.File

/**
 * Строка счётчика A1: label + значение справа, акцент, высота 56.
 * С [onClick] — кнопка перехода: строка кликабельна, справа шеврон.
 */
@Composable
fun StatRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    val palette = LocalGymPalette.current
    val clickable = onClick != null && enabled
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(palette.panel)
            .then(if (clickable) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 17.sp, color = palette.text)
        Spacer(Modifier.weight(1f))
        Text(
            value,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = palette.accent,
        )
        if (clickable) {
            Text(
                "›",
                fontSize = 17.sp,
                color = palette.muted,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

/** Акцентная кнопка основного действия, высота 52. */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val palette = LocalGymPalette.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(if (enabled) palette.accent else palette.panel2)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) palette.onAccent else palette.muted,
        )
    }
}

/** Контурная кнопка второстепенного действия, высота 52. */
@Composable
fun OutlinedButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val palette = LocalGymPalette.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(11.dp))
            .border(1.dp, if (enabled) palette.accent else palette.line, RoundedCornerShape(11.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) palette.accent else palette.muted,
        )
    }
}

/** Малая контурная кнопка, высота 44 («Завершить упражнение», «Отмена»). */
@Composable
fun OutlinedSmallButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = LocalGymPalette.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, palette.accent, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = palette.accent)
    }
}

/** Малая контурная кнопка удаления («Удалить тренировку»). */
@Composable
fun DangerSmallButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = LocalGymPalette.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, palette.dangerBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = palette.danger)
    }
}

/** bubble-подход: «5 × 85 кг». */
@Composable
fun SetChip(text: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val palette = LocalGymPalette.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(palette.field)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(text, fontSize = 13.sp, color = palette.text)
    }
}

/** Миниатюра фото упражнения; placeholder — панель цвета фото мокапа. */
@Composable
fun ExerciseThumb(imagePath: String?, modifier: Modifier = Modifier) {
    val palette = LocalGymPalette.current
    Box(
        modifier = modifier
            .size(52.dp, 42.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(palette.photo),
        contentAlignment = Alignment.Center,
    ) {
        if (imagePath != null) {
            AsyncImage(
                model = File(imagePath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** Заголовок верхнего экрана. */
@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier) {
    val palette = LocalGymPalette.current
    Text(
        text,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
        fontSize = 19.sp,
        fontWeight = FontWeight.SemiBold,
        color = palette.text,
    )
}

/** Подзаголовок: дата и число упражнений. */
@Composable
fun ScreenSub(text: String, modifier: Modifier = Modifier) {
    val palette = LocalGymPalette.current
    Text(
        text,
        modifier = modifier.padding(start = 16.dp, end = 16.dp),
        fontSize = 13.sp,
        color = palette.muted,
    )
}

/** Заголовок возврата во вложенном экране («← Журнал»). */
@Composable
fun BackHeader(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = LocalGymPalette.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 2.dp)
            .clickable(onClick = onClick),
    ) {
        Text(text, fontSize = 15.sp, color = palette.muted)
    }
}

/** Диалог A5/A6: заголовок по центру, акцентная кнопка, контурная — без поясняющего текста. */
@Composable
fun ConfirmDialog(
    title: String,
    primaryText: String,
    secondaryText: String,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
) {
    val palette = LocalGymPalette.current
    androidx.compose.ui.window.Dialog(onDismissRequest = onSecondary) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(palette.panel2)
                .padding(16.dp),
        ) {
            Text(
                title,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = palette.text,
            )
            PrimaryButton(primaryText, onClick = onPrimary)
            Spacer(Modifier.height(8.dp))
            OutlinedSmallButton(secondaryText, onClick = onSecondary)
        }
    }
}

/** Диалог подтверждения с текстом (удаление). */
@Composable
fun TextConfirmDialog(
    title: String,
    message: String,
    primaryText: String,
    secondaryText: String,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
) {
    val palette = LocalGymPalette.current
    androidx.compose.ui.window.Dialog(onDismissRequest = onSecondary) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(palette.panel2)
                .padding(16.dp),
        ) {
            Text(
                title,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.text,
            )
            Text(
                message,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                fontSize = 14.sp,
                color = palette.muted,
            )
            PrimaryButton(primaryText, onClick = onPrimary)
            Spacer(Modifier.height(8.dp))
            OutlinedSmallButton(secondaryText, onClick = onSecondary)
        }
    }
}

/** Круглая иконка-кнопка («↻», плюс в круге). */
@Composable
fun RoundIconButton(symbol: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = LocalGymPalette.current
    Box(
        modifier = modifier
            .size(38.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .border(1.dp, palette.accent, androidx.compose.foundation.shape.CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, fontSize = 18.sp, color = palette.accent)
    }
}
