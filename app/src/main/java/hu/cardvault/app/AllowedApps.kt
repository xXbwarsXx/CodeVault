package hu.cardvault.app

import android.content.Context

internal object AllowedApps {
    private const val KEY = "allowed_packages"
    private fun prefs(ctx: Context) = ctx.getSharedPreferences("autofill_allowed", Context.MODE_PRIVATE)
    fun get(ctx: Context): Set<String> = prefs(ctx).getStringSet(KEY, emptySet())?.toSet().orEmpty()
    fun isAllowed(ctx: Context, pkg: String) = pkg in get(ctx)
    fun set(ctx: Context, pkg: String, allowed: Boolean) {
        val updated = get(ctx).toMutableSet()
        if (allowed) updated.add(pkg) else updated.remove(pkg)
        prefs(ctx).edit().putStringSet(KEY, updated).apply()
    }
}
