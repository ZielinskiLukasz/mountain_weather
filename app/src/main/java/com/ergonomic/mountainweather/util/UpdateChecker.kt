package com.ergonomic.mountainweather.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

class UpdateState(
    private val manager: AppUpdateManager,
    private val activity: Activity?
) {
    var available: Boolean by mutableStateOf(false)
        internal set
    var downloaded: Boolean by mutableStateOf(false)
        internal set
    var dismissed: Boolean by mutableStateOf(false)
        internal set

    val visible: Boolean get() = (available || downloaded) && !dismissed

    private var pendingLauncher: ((IntentSenderRequest) -> Unit)? = null

    internal fun setLauncher(launcher: (IntentSenderRequest) -> Unit) {
        pendingLauncher = launcher
    }

    fun startUpdate() {
        val act = activity ?: return
        manager.appUpdateInfo
            .addOnSuccessListener { info ->
                val canStart = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                if (canStart) {
                    manager.startUpdateFlowForResult(
                        info,
                        act,
                        AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
                        UPDATE_REQUEST_CODE
                    )
                } else {
                    openPlayStore(act)
                }
            }
            .addOnFailureListener { openPlayStore(act) }
    }

    private fun openPlayStore(act: Activity) {
        val pkg = act.packageName
        val marketUri = Uri.parse("market://details?id=$pkg")
        runCatching {
            act.startActivity(
                Intent(Intent.ACTION_VIEW, marketUri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.onFailure {
            runCatching {
                val webUri = Uri.parse("https://play.google.com/store/apps/details?id=$pkg")
                act.startActivity(
                    Intent(Intent.ACTION_VIEW, webUri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }

    fun completeUpdate() {
        manager.completeUpdate()
    }

    fun dismiss() {
        dismissed = true
    }

    companion object {
        const val UPDATE_REQUEST_CODE = 9001
    }
}

@Composable
fun rememberUpdateState(): UpdateState {
    val context = LocalContext.current
    val activity = context.findActivity()
    val manager = remember(context) { AppUpdateManagerFactory.create(context.applicationContext) }
    val state = remember(manager) { UpdateState(manager, activity) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { /* result is reflected in InstallStateUpdatedListener */ }
    state.setLauncher { req -> launcher.launch(req) }

    LaunchedEffect(manager) {
        runCatching {
            manager.appUpdateInfo.addOnSuccessListener { info ->
                val isAvailable = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                val canFlexible = info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                if (isAvailable && canFlexible) {
                    state.available = true
                }
                if (info.installStatus() == InstallStatus.DOWNLOADED) {
                    state.downloaded = true
                }
            }
        }
    }

    val listener = remember {
        InstallStateUpdatedListener { installState ->
            if (installState.installStatus() == InstallStatus.DOWNLOADED) {
                state.downloaded = true
            }
        }
    }

    LaunchedEffect(manager, listener) {
        runCatching { manager.registerListener(listener) }
    }

    return state
}

private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is android.content.ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
