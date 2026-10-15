package app.template.patches.gmail

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import app.template.patches.shared.Constants.GMAIL_GMSCORE_COMPATIBILITY
import app.template.patches.shared.cert.autoSha1
import app.template.patches.shared.cert.extractApkCertificatePatch
import org.w3c.dom.Element

private const val GMAIL_PACKAGE = "com.google.android.gm"

/**
 * Experimental, Gmail-scoped GmsCore manifest metadata ONLY.
 *
 * Only targets Gmail 2026.09.28.994593356.Release (versionCode 66072052).
 * Target was identified from public distribution metadata and is NOT yet tested.
 * This intentionally does NOT rewrite DEX references, spoof Play Integrity,
 * bypass login checks, replace GMS, or modify FCM token registration.
 * The universal GmsCore bytecode patch must NOT be selected together with this
 * experiment until a specific original Gmail APK has been tested.
 */
@Suppress("unused")
val gmailGmsCoreMetadataPatch = resourcePatch(
    name = "Gmail GmsCore metadata (experimental)",
    description = "Adds MicroG/GmsCore visibility and signature-spoof metadata to Gmail's Manifest only. Does NOT support login, sync or push notifications yet. Do NOT install over your normal Gmail.",
    default = false,
) {
    compatibleWith(GMAIL_GMSCORE_COMPATIBILITY)

    val targetGmsCorePackage by stringOption(
        key = "gmailGmsCorePackage",
        default = "app.revanced.android.gms",
        values = mapOf(
            "MicroG RE (app.revanced.android.gms)" to "app.revanced.android.gms",
            "Morphe GmsCore (app.morphe.android.gms)" to "app.morphe.android.gms",
        ),
        title = "GmsCore package",
        description = "Must match the package name of GmsCore installed on the test device.",
        required = true,
    )

    // Derive the original signing certificate from the unmodified Gmail input.
    dependsOn(extractApkCertificatePatch)

    execute {
        val gmsCorePackage = targetGmsCorePackage ?: error("GmsCore package was not provided")
        val vendorGroup = gmsCorePackage.removeSuffix(".android.gms")
        val certificateSha1 = autoSha1?.takeIf {
            it.matches(Regex("^[0-9a-fA-F]{40}$"))
        } ?: error(
            "Original APK SHA-1 certificate unavailable. Supply an unmodified Gmail APK " +
                "to the 'Provide Original app certificate' patch."
        )

        document("AndroidManifest.xml").use { document ->
            val manifest = document.getElementsByTagName("manifest").item(0) as? Element
                ?: error("Gmail AndroidManifest is missing the manifest element")
            require(manifest.getAttribute("package") == GMAIL_PACKAGE) {
                "This experimental patch only supports the original Gmail package: $GMAIL_PACKAGE"
            }
            val application = document.getElementsByTagName("application").item(0) as? Element
                ?: error("Gmail AndroidManifest is missing the application element")

            // Allows Gmail to query MicroG on Android 11+.
            val queries = (document.getElementsByTagName("queries").item(0) as? Element)
                ?: document.createElement("queries").also { manifest.insertBefore(it, application) }
            val knownPackages = queries.getElementsByTagName("package")
            val hasGmsQuery = (0 until knownPackages.length).any { index ->
                (knownPackages.item(index) as? Element)?.getAttribute("android:name") == gmsCorePackage
            }
            if (!hasGmsQuery) {
                queries.appendChild(document.createElement("package").apply {
                    setAttribute("android:name", gmsCorePackage)
                })
            }

            val permissions = document.getElementsByTagName("uses-permission")
            val fakePermission = "org.microg.gms.permission.FAKE_PACKAGE_SIGNATURE"
            val hasFakePermission = (0 until permissions.length).any { index ->
                (permissions.item(index) as? Element)?.getAttribute("android:name") == fakePermission
            }
            if (!hasFakePermission) {
                manifest.insertBefore(document.createElement("uses-permission").apply {
                    setAttribute("android:name", fakePermission)
                }, application)
            }

            fun upsertMetadata(key: String, value: String) {
                val existing = (0 until application.childNodes.length)
                    .mapNotNull { application.childNodes.item(it) as? Element }
                    .firstOrNull { it.tagName == "meta-data" && it.getAttribute("android:name") == key }
                if (existing != null) {
                    existing.setAttribute("android:value", value)
                } else {
                    application.appendChild(document.createElement("meta-data").apply {
                        setAttribute("android:name", key)
                        setAttribute("android:value", value)
                    })
                }
            }

            // Metadata pattern used by the upstream Google Photos GmsCore patch.
            upsertMetadata("$gmsCorePackage.SPOOFED_PACKAGE_NAME", GMAIL_PACKAGE)
            upsertMetadata("$gmsCorePackage.SPOOFED_PACKAGE_SIGNATURE", certificateSha1)
            upsertMetadata("$vendorGroup.MICROG_PACKAGE_NAME", gmsCorePackage)
        }

        println(
            "Gmail GmsCore experiment: Manifest metadata inserted for $gmsCorePackage; " +
                "DEX, authentication and FCM remain unchanged."
        )
    }
}
