package com.lsykje.diary.auth

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/** 설계 문서 7-1절: 안드로이드 표준 BiometricPrompt를 그대로 사용(제조사 지문 센서를 직접 다루지 않음) */
object BiometricAuthenticator {

    fun isAvailable(activity: FragmentActivity): Boolean {
        val manager = BiometricManager.from(activity)
        val result = manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        return result == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun authenticate(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onFailOrError: () -> Unit,
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onFailOrError()
                }

                override fun onAuthenticationFailed() {
                    // 오인식 1회 실패는 무시 — 사용자가 다시 시도할 수 있도록 BiometricPrompt가 알아서 재시도 UI를 보여줌
                }
            },
        )

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("나의 일기장 잠금 해제")
            .setSubtitle("지문 또는 얼굴로 인증해주세요")
            .setNegativeButtonText("다른 방법으로 잠금 해제")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        prompt.authenticate(info)
    }
}
