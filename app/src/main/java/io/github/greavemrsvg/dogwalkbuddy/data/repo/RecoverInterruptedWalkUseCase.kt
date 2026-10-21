package io.github.greavemrsvg.dogwalkbuddy.data.repo

/**
 * At app start, saves any walk still marked `RECORDING` other than the one live in this process, ending
 * it at its last recorded fix (ADR 0004, AC34). The process-local `RecordingStateHolder` is empty in a fresh
 * process, which is what marks such a walk as interrupted. Returns how many walks were recovered.
 */
class RecoverInterruptedWalkUseCase(
    private val repository: WalkRepository,
    private val liveWalkId: () -> Long?,
) {
    suspend operator fun invoke(): Int {
        val live = liveWalkId()
        val interrupted = repository.recordingWalks().filter { it.id != live }
        interrupted.forEach { repository.completeAtLastFix(it) }
        return interrupted.size
    }
}
