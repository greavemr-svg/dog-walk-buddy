package io.github.greavemrsvg.dogwalkbuddy.testutil

import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.app.ActivityOptionsCompat

/**
 * Answers permission requests the way the system dialog would, so a screen test can grant or refuse without a real
 * dialog. [answer] receives the permissions asked for and returns the ones that are granted.
 */
class FakePermissionRegistry(
    private val answer: (List<String>) -> Set<String>,
) : ActivityResultRegistry() {
    /** Every request made, in order. */
    val requests = mutableListOf<List<String>>()

    @Suppress("UNCHECKED_CAST")
    override fun <I : Any?, O : Any?> onLaunch(
        requestCode: Int,
        contract: ActivityResultContract<I, O>,
        input: I,
        options: ActivityOptionsCompat?,
    ) {
        when (contract) {
            is ActivityResultContracts.RequestPermission -> {
                val permission = input as String
                requests += listOf(permission)
                dispatchResult(requestCode, answer(listOf(permission)).contains(permission) as O)
            }
            is ActivityResultContracts.RequestMultiplePermissions -> {
                val permissions = (input as Array<String>).toList()
                requests += permissions
                val granted = answer(permissions)
                dispatchResult(requestCode, permissions.associateWith { granted.contains(it) } as O)
            }
            else -> error("Unexpected contract ${contract.javaClass.name}")
        }
    }
}

@Composable
fun WithPermissionRegistry(
    registry: FakePermissionRegistry,
    content: @Composable () -> Unit,
) {
    val owner =
        object : ActivityResultRegistryOwner {
            override val activityResultRegistry: ActivityResultRegistry = registry
        }
    CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner, content = content)
}
