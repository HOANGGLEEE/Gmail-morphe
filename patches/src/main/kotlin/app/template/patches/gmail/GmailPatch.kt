package app.template.patches.gmail

import app.morphe.patcher.patch.resourcePatch
import app.template.patches.shared.Constants.GMAIL_COMPATIBILITY

/**
 * Registers Gmail as an experimental app target in Morphe Manager.
 *
 * Deliberately DOES NOT touch the Gmail APK, GMS, Firebase Cloud Messaging,
 * package name, or authentication. A signed/repacked APK can still be unsafe
 * to install, even if this patch performs no modifications.
 */
@Suppress("unused")
val gmailRegistrationPatch = resourcePatch(
    name = "Gmail registration (experimental)",
    description = "Lists Gmail for research only. No GmsCore, login, sync, or FCM changes. Do not install a re-signed Gmail APK to fix notifications.",
    default = false,
) {
    compatibleWith(GMAIL_COMPATIBILITY)

    execute {
        println("Gmail registration only: no APK modifications performed.")
    }
}
