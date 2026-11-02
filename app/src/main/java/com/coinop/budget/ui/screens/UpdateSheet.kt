package com.coinop.budget.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coinop.budget.BuildConfig
import com.coinop.budget.data.UpdateUi
import com.coinop.budget.ui.BlockBar
import com.coinop.budget.ui.MainViewModel
import com.coinop.budget.ui.RText
import com.coinop.budget.ui.RetroButton
import com.coinop.budget.ui.RetroSheet
import com.coinop.budget.ui.theme.Retro

/** Bottom sheet behind Settings > About > Check for updates. */
@Composable
fun UpdateSheet(vm: MainViewModel, onDismiss: () -> Unit) {
    val p = Retro.p
    val state = vm.update

    // Opening the sheet runs the check straight away.
    LaunchedEffect(Unit) { if (vm.update is UpdateUi.Idle || vm.update is UpdateUi.UpToDate || vm.update is UpdateUi.Failed) vm.checkForUpdate() }

    RetroSheet({ vm.resetUpdate(); onDismiss() }) {
        RText("APP UPDATE", size = 16.sp, bold = true)
        RText("Installed: v${BuildConfig.VERSION_NAME}", size = 12.sp, color = p.onVariant)
        Spacer(Modifier.height(4.dp))

        when (state) {
            is UpdateUi.Idle, is UpdateUi.Checking -> {
                RText("Checking GitHub for the latest release...", color = p.onVariant)
            }
            is UpdateUi.UpToDate -> {
                RText("You are on the latest version.", bold = true)
                RetroButton("Check again", { vm.checkForUpdate() }, Modifier.fillMaxWidth())
            }
            is UpdateUi.Available -> {
                RText("Version ${state.release.version} is available", bold = true)
                if (state.release.size > 0) {
                    RText("Download size: ${"%.1f".format(state.release.size / 1_048_576.0)} MB", size = 12.sp, color = p.onVariant)
                }
                if (state.release.notes.isNotEmpty()) {
                    RText(state.release.notes.take(400), size = 12.sp, color = p.onVariant)
                }
                RetroButton("Download", { vm.downloadUpdate() }, Modifier.fillMaxWidth())
            }
            is UpdateUi.Downloading -> {
                RText("Downloading v${state.release.version}... ${(state.progress * 100).toInt()}%", bold = true)
                BlockBar(state.progress, p.mustard, Modifier.fillMaxWidth())
            }
            is UpdateUi.Ready -> {
                RText("v${state.release.version} is ready to install.", bold = true)
                RText(
                    "Android will ask you to confirm. The first time, it also asks you to allow installs from Coinop.",
                    size = 12.sp,
                    color = p.onVariant,
                )
                RetroButton("Install", { vm.installUpdate() }, Modifier.fillMaxWidth())
            }
            is UpdateUi.Failed -> {
                RText(state.message, bold = true)
                RetroButton("Try again", { vm.checkForUpdate() }, Modifier.fillMaxWidth())
            }
        }
    }
}
