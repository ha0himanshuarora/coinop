package com.coinop.budget.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coinop.budget.data.Category
import com.coinop.budget.domain.TimeUtil
import com.coinop.budget.ui.Chip
import com.coinop.budget.ui.ChipFlow
import com.coinop.budget.ui.IconBox
import com.coinop.budget.ui.RText
import com.coinop.budget.ui.theme.AppIcon
import com.coinop.budget.ui.theme.Retro
import java.time.LocalDate

/** All categories as chips. Tapping the selected chip keeps it selected (no "none" state). */
@Composable
fun CategoryChips(
    categories: List<Category>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    ChipFlow(modifier) {
        categories.forEach { c ->
            Chip(c.name, selected = c.id == selectedId, onClick = { onSelect(c.id) })
        }
    }
}

/** Day picker without a calendar dialog: previous day, date, next day. */
@Composable
fun DateStepper(date: LocalDate, onChange: (LocalDate) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconBox(AppIcon.MINUS, { onChange(date.minusDays(1)) })
        Box(Modifier.weight(1f).padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
            RText(
                date.format(TimeUtil.dayLabel).uppercase(),
                bold = true,
                size = 14.sp,
                textAlign = TextAlign.Center,
            )
        }
        IconBox(AppIcon.PLUS, { onChange(date.plusDays(1)) })
    }
}

@Composable
fun Gap(w: Int = 8) {
    Spacer(Modifier.width(w.dp))
}

@Composable
fun ButtonRow(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
fun mutedColor() = Retro.p.onVariant
