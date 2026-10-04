package com.shilapi.xcertplay

import android.content.Context
import com.shilapi.xcertplay.mfi.LocalMfiAuthenticationClient
import com.shilapi.xcertplay.orchestration.MfiTarget
import java.io.File
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

/**
 * Source-only test builds ship no provisioned MFi identity, and this fork offers local offline
 * authentication only. The shadow mirrors the real precondition instead of certificate material:
 * [DiPlayBootstrap.ensure] succeeds once the app-private `offline-mfi` directory exists, so tests can
 * cover both the provisioned and the missing-identity path without bundling credentials.
 *
 * Keep this the module's only shadow of `DiPlayBootstrap`: Robolectric keeps one shadow instance per
 * shadowed class in a sandbox, so a second shadow class makes the other call sites cast to the wrong
 * shadow type (`ClassCastException`).
 */
@Implements(DiPlayBootstrap::class, isInAndroidSdk = false)
class DiPlayBootstrapShadow {
    @Implementation fun ensure(context: Context, mfiTarget: MfiTarget) {
        if (mfiTarget != MfiTarget.LOCAL) return
        check(File(context.noBackupFilesDir, LocalMfiAuthenticationClient.DIRECTORY).isDirectory) {
            "offline-mfi/identity.pk8"
        }
    }
}
