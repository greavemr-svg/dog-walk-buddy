package io.github.greavemrsvg.dogwalkbuddy.ui.history

import android.content.Context
import io.github.greavemrsvg.dogwalkbuddy.AppContainer
import io.github.greavemrsvg.dogwalkbuddy.R
import io.github.greavemrsvg.dogwalkbuddy.data.db.WalkDao
import io.github.greavemrsvg.dogwalkbuddy.data.db.WalkEntity
import io.github.greavemrsvg.dogwalkbuddy.data.db.WalkStatus
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import kotlin.random.Random

/** Debug-build actions in the History overflow menu. The release build has none. */
object DebugTools {
    const val SAMPLE_WALK_COUNT = 500

    fun historyActions(
        context: Context,
        container: AppContainer,
    ): List<DebugAction> =
        listOf(
            DebugAction(context.getString(R.string.debug_seed_walks)) {
                container.applicationScope.launch { seed(container.database.walkDao(), container.clock.instant()) }
            },
        )

    /** Inserts [SAMPLE_WALK_COUNT] synthetic finished walks, one about every 8 hours back from [now]. Not real data. */
    suspend fun seed(
        dao: WalkDao,
        now: Instant,
        count: Int = SAMPLE_WALK_COUNT,
    ) {
        val random = Random(SEED)
        repeat(count) { index ->
            val start = now.minus(Duration.ofHours(SPACING_HOURS * (index + 1L)))
            val minutes = MIN_MINUTES + random.nextLong(MINUTE_SPREAD)
            dao.insertWalk(
                WalkEntity(
                    startUtcMs = start.toEpochMilli(),
                    endUtcMs = start.plus(Duration.ofMinutes(minutes)).toEpochMilli(),
                    status = WalkStatus.COMPLETE,
                    distanceM = minutes * METRES_PER_MINUTE,
                ),
            )
        }
    }

    private const val SEED = 7
    private const val SPACING_HOURS = 8L
    private const val MIN_MINUTES = 10L
    private const val MINUTE_SPREAD = 80L
    private const val METRES_PER_MINUTE = 80.0
}
