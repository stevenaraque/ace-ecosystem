package com.ace.mobile.service.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.ace.mobile.domain.usecase.exercise.SendPendingBlocksUseCase
import com.ace.shared.constants.SyncConstants
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

private const val TAG = "SyncBlockWorker"

@HiltWorker
class SyncBlockWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val sendPendingBlocksUseCase: SendPendingBlocksUseCase
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "sync_block_worker"
        const val KEY_SESSION_ID = "session_id"
        const val KEY_RESULT_SYNCED = "result_synced_count"
        const val KEY_RESULT_ERRORS = "result_error_count"

        fun enqueue(context: Context, sessionId: String? = null) {
            val inputData = sessionId?.let {
                workDataOf(KEY_SESSION_ID to it)
            } ?: Data.EMPTY

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<SyncBlockWorker>()
                .setInputData(inputData)
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    SyncConstants.RETRY_DELAY_INITIAL_MS,
                    TimeUnit.MILLISECONDS
                )
                .addTag(WORK_NAME)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )

            Log.i(TAG, "SyncBlockWorker enqueued for session=$sessionId")
        }
    }

    override suspend fun doWork(): Result {
        val sessionId = inputData.getString(KEY_SESSION_ID)
        val runAttempt = runAttemptCount + 1

        Log.i(TAG, "SyncBlockWorker started (attempt $runAttempt/${SyncConstants.MAX_RETRIES})")

        if (runAttempt > SyncConstants.MAX_RETRIES) {
            Log.e(TAG, "Max retry attempts (${SyncConstants.MAX_RETRIES}) exceeded")
            return Result.failure(
                workDataOf("error" to "Max retries exceeded")
            )
        }

        return try {
            when (val useCaseResult = sendPendingBlocksUseCase(sessionId)) {
                is SendPendingBlocksUseCase.Result.Success -> {
                    Log.i(TAG, "Sync successful: ${useCaseResult.syncedCount} synced, ${useCaseResult.errorCount} errors")

                    if (useCaseResult.syncedCount > 0) {
                        enqueue(applicationContext, sessionId)
                    }

                    Result.success(
                        workDataOf(
                            KEY_RESULT_SYNCED to useCaseResult.syncedCount,
                            KEY_RESULT_ERRORS to useCaseResult.errorCount
                        )
                    )
                }

                is SendPendingBlocksUseCase.Result.NoPendingBlocks -> {
                    Log.d(TAG, "No pending blocks, worker finishing")
                    Result.success()
                }

                is SendPendingBlocksUseCase.Result.NetworkError -> {
                    Log.w(TAG, "Network error, will retry (attempt $runAttempt)")
                    Result.retry()
                }

                is SendPendingBlocksUseCase.Result.AuthError -> {
                    Log.e(TAG, "Auth error, not retrying")
                    Result.failure(
                        workDataOf("error" to useCaseResult.message)
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in SyncBlockWorker", e)
            Result.retry()
        }
    }
}