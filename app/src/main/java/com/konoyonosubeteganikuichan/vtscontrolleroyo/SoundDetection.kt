package com.konoyonosubeteganikuichan.vtscontrolleroyo

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max


class SoundDetection() {

    // サンプリングレート (Hz)
    private val samplingRate = 44100
    private var FFT_SIZE = 4096

    // デシベルベースラインの設定
    val dB_baseline = Math.pow(2.0, 15.0) * FFT_SIZE * Math.sqrt(2.0)

    // 分解能の計算
    val resol: Double = samplingRate / FFT_SIZE.toDouble()

    // フレームレート (fps)
    private val frameRate = 10

    // 1フレームの音声データ(=Short値)の数
    private val oneFrameDataCount = samplingRate / frameRate

    // 1フレームの音声データのバイト数 (byte)
    // Byte = 8 bit, Short = 16 bit なので, Shortの倍になる
    private val oneFrameSizeInByte = oneFrameDataCount * 2

    // 音声データのバッファサイズ (byte)
    // 要件1:oneFrameSizeInByte より大きくする必要がある
    // 要件2:デバイスの要求する最小値より大きくする必要がある
    private val audioBufferSizeInByte =
        max(oneFrameSizeInByte * 10, // 適当に10フレーム分のバッファを持たせた
            android.media.AudioRecord.getMinBufferSize(samplingRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT))
    lateinit var audioRecord: AudioRecord

    fun startRecording() {

        // インスタンスの作成
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC, // 音声のソース
            samplingRate, // サンプリングレート
            AudioFormat.CHANNEL_IN_MONO, // チャネル設定. MONO and STEREO が全デバイスサポート保障
            AudioFormat.ENCODING_PCM_16BIT, // PCM16が全デバイスサポート保障
            audioBufferSizeInByte) // バッファ

        // 音声データを幾つずつ処理するか( = 1フレームのデータの数)
        audioRecord.positionNotificationPeriod = oneFrameDataCount

        // ここで指定した数になったタイミングで, 後続の onMarkerReached が呼び出される
        // 通常のストリーミング処理では必要なさそう？
        audioRecord.notificationMarkerPosition = 40000 // 使わないなら設定しない.

        // 音声データを格納する配列
        val audioDataArray = ShortArray(oneFrameDataCount)

        // コールバックを指定
        audioRecord.setRecordPositionUpdateListener(object : AudioRecord.OnRecordPositionUpdateListener {

            // フレームごとの処理
            override fun onPeriodicNotification(recorder: AudioRecord) {
                recorder.read(audioDataArray, 0, oneFrameDataCount) // 音声データ読込
                Log.v("AudioRecord", "onPeriodicNotification size=${audioDataArray.size}")
                // 好きに処理する
                //エンディアン変換
                //エンディアン変換
                //FFTクラスの作成と値の引き渡し https://gist.github.com/qwasd1224k/ccf4e793677348e05984
                val fft = FFT4g(FFT_SIZE)
                val FFTdata = DoubleArray(FFT_SIZE)
                for (i in 0 until FFT_SIZE) {
                    FFTdata[i] = audioDataArray[i].toDouble()
                }
                fft.rdft(1, FFTdata)

                // デシベルの計算
                val dbfs = DoubleArray(FFT_SIZE / 2)
                var max_db = -120.0
                var max_i = 0
                var i = 0
                while (i < FFT_SIZE) {
                    dbfs[i / 2] = (20 * Math.log10(Math.sqrt(Math.pow(FFTdata[i], 2.0) + Math.pow(FFTdata[i + 1], 2.0)) / dB_baseline))
                    if (max_db < dbfs[i / 2]) {
                        max_db = dbfs[i / 2]
                        max_i = i / 2
                    }
                    i += 2
                }

                //音量が最大の周波数と，その音量を表示
                Log.d(
                    "fft",
                    "周波数：" + (resol * max_i).toString() + " [Hz] 音量：" + max_db.toString() + " [dB]"
                )
            }

            // マーカータイミングの処理.
            // notificationMarkerPosition に到達した際に呼ばれる
            override fun onMarkerReached(recorder: AudioRecord) {
                recorder.read(audioDataArray, 0, oneFrameDataCount) // 音声データ読込
                Log.v("AudioRecord", "onMarkerReached size=${audioDataArray.size}")
                // 好きに処理する
            }
        })

        audioRecord.startRecording()
    }

    fun stopRecording() {
        audioRecord.stop()
    }
}