package com.practicetracker.ui.practice

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Drives the metronome via AudioTrack in streaming mode.
 *
 * The write loop pushes exactly one beat's worth of PCM samples per iteration.
 * AudioTrack.write() in blocking mode throttles naturally to the hardware audio
 * clock, so timing error does not accumulate the way coroutine delay() would.
 *
 * Sequence per beat:
 *   1. Update _currentBeat (visual fires ~minBufferLatency ms before the click)
 *   2. Build beat buffer: click PCM followed by silence for the rest of the beat
 *   3. write() — blocks until the audio hardware has consumed the previous beat's
 *      tail, then returns once this beat's samples are queued. The hardware clock
 *      owns all timing from here.
 */
class MetronomeManager {

    companion object {
        const val BPM_MIN = 40
        const val BPM_MAX = 240
        private const val SAMPLE_RATE = 44100
        private const val CLICK_SAMPLES = SAMPLE_RATE / 20  // 50 ms click
    }

    private val _currentBeat = MutableStateFlow(0)
    val currentBeat: StateFlow<Int> = _currentBeat.asStateFlow()

    @Volatile var bpm: Int = 80
        private set
    @Volatile var beatsPerMeasure: Int = 4
        private set

    private var job: Job? = null
    private var audioTrack: AudioTrack? = null

    // Pre-generated click waveforms — never change after construction.
    private val accentPcm: ShortArray = generateClick(isAccent = true)
    private val normalPcm: ShortArray = generateClick(isAccent = false)

    val isPlaying: Boolean get() = job?.isActive == true

    fun start(scope: CoroutineScope) {
        if (isPlaying) return

        val track = buildStreamingTrack()
        audioTrack = track
        track.play()

        job = scope.launch(Dispatchers.IO) {
            var beat = 0
            while (isActive) {
                val beatIndex = beat % beatsPerMeasure

                // Update visual before write so the dot lights up as the click is queued.
                // It fires ~minBufferLatency (≈ 20 ms) before the click is heard — imperceptible.
                _currentBeat.value = beatIndex

                val samplesPerBeat = (SAMPLE_RATE * 60.0 / bpm).roundToInt()
                val buffer = buildBeatBuffer(beatIndex, samplesPerBeat)

                // Blocking write: returns once all samples are in the ring buffer.
                // The ring buffer drains at hardware clock rate, so our loop is
                // automatically paced to real time with no drift.
                val written = track.write(buffer, 0, buffer.size)
                if (written <= 0) break   // track was paused/released externally

                beat++
            }
        }
    }

    fun stop() {
        // Pause before cancelling so that a blocking write() unblocks immediately.
        val track = audioTrack
        audioTrack = null
        runCatching { track?.pause() }
        runCatching { track?.flush() }
        job?.cancel()
        job = null
        _currentBeat.value = 0
        runCatching { track?.release() }
    }

    fun release() = stop()

    fun setBpm(newBpm: Int) {
        bpm = newBpm.coerceIn(BPM_MIN, BPM_MAX)
        // No restart: the next beat's write() automatically uses the updated value.
    }

    fun setBeatsPerMeasure(beats: Int) {
        beatsPerMeasure = beats.coerceIn(1, 16)
        _currentBeat.value = 0
    }

    private fun buildStreamingTrack(): AudioTrack {
        val minBuf = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        // 2× min buffer gives the hardware a little headroom without adding
        // noticeable latency to the visual indicator.
        val bufBytes = maxOf(minBuf * 2, 4096)
        return AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
            AudioFormat.Builder()
                .setSampleRate(SAMPLE_RATE)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build(),
            bufBytes,
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
    }

    private fun buildBeatBuffer(beatIndex: Int, samplesPerBeat: Int): ShortArray {
        val buffer = ShortArray(samplesPerBeat)   // silence by default
        val click = if (beatIndex == 0) accentPcm else normalPcm
        click.copyInto(buffer, destinationOffset = 0, startIndex = 0,
            endIndex = minOf(click.size, buffer.size))
        return buffer
    }

    private fun generateClick(isAccent: Boolean): ShortArray {
        val freq = if (isAccent) 1800.0 else 1200.0
        val amplitude = Short.MAX_VALUE * 0.75
        return ShortArray(CLICK_SAMPLES) { i ->
            val envelope = 1.0 - i.toDouble() / CLICK_SAMPLES
            (amplitude * envelope * sin(2 * PI * freq * i / SAMPLE_RATE)).toInt().toShort()
        }
    }
}
