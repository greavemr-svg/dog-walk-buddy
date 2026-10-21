package io.github.greavemrsvg.dogwalkbuddy.data.repo

/**
 * At app start, saves any walk still marked `RECORDING` when no recording service is live in this process, ending
 * it at its last recorded fix (ADR 0004, AC34). The process-local `RecordingStateHolder` is empty in a fresh
 * process, which is what marks such a walk as interrupted. Returns how many walks were recovered.
 */
class RecoverInterruptedWalkUseCase(
    private val repository: WalkRepository,
    private val isRecordingLive: () -> Boolean,
) {
    suspend operator fun invoke(): Int {
        if (isRecordingLive()) return 0
        val interrupted = repository.recordingWalks()
        interrupted.forEach { repository.completeAtLastFix(it) }
        return interrupted.size
    }
}
