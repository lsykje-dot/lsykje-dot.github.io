package com.lsykje.diary.voice

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

/**
 * 음성으로 일기 쓰기 기능의 핵심 규칙: 오디오가 기기 밖으로 나가지 않는
 * "온디바이스" 음성 인식만 사용한다.
 *
 * - 안드로이드 12(API 31) 이상 + 기기에 온디바이스 인식 모델이 설치돼 있어야 지원됨
 * - 조건을 만족하지 않는 기기에서는 이 기능 자체를 끈다(온라인 방식으로 대체하지 않음) —
 *   그래야 "인터넷을 거치지 않는 로컬 전용 앱"이라는 원칙이 깨지지 않는다
 */
object VoiceInputRecognizer {

    fun isSupported(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
        return SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
    }

    fun buildIntent(): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.KOREA.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }

    /** isSupported(context)가 true일 때만 호출할 것. 아니면 null을 돌려준다 */
    fun create(
        context: Context,
        onPartialResult: (String) -> Unit,
        onFinalResult: (String) -> Unit,
        onListeningChanged: (Boolean) -> Unit,
        onErrorText: (String) -> Unit,
    ): SpeechRecognizer? {
        if (!isSupported(context)) return null

        val recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                onListeningChanged(true)
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                onListeningChanged(false)
            }

            override fun onError(error: Int) {
                onListeningChanged(false)
                onErrorText(errorMessage(error))
            }

            override fun onResults(results: Bundle?) {
                onListeningChanged(false)
                results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.takeIf { it.isNotBlank() }
                    ?.let(onFinalResult)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.takeIf { it.isNotBlank() }
                    ?.let(onPartialResult)
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        return recognizer
    }

    private fun errorMessage(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_NO_MATCH -> "알아듣지 못했어요. 다시 말씀해주세요"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "말씀이 없어서 종료됐어요"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "마이크 권한이 필요해요"
        SpeechRecognizer.ERROR_AUDIO -> "마이크를 사용할 수 없어요"
        else -> "음성 인식 중 문제가 발생했어요"
    }
}
