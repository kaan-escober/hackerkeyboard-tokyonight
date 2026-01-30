/*
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.pocketworkstation.pckeyboard

import android.view.MotionEvent
import kotlin.math.max
import kotlin.math.min

/**
 * Tracks finger swipe motion by recording movement history and computing velocity.
 * Uses a 4-event ring buffer to maintain the most recent touch points, allowing
 * velocity calculations based on the movement between events.
 */
internal class SwipeTracker {
    private val mBuffer = EventRingBuffer(NUM_PAST)

    private var mYVelocity: Float = 0f
    private var mXVelocity: Float = 0f

    /**
     * Records a motion event in the movement history buffer.
     * On ACTION_DOWN, clears the buffer and returns. For other actions,
     * processes all historical events and the current event within the
     * time window to maintain velocity computation accuracy.
     *
     * @param ev the MotionEvent to record
     */
    fun addMovement(ev: MotionEvent) {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            mBuffer.clear()
            return
        }
        val time = ev.eventTime
        val count = ev.historySize
        for (i in 0 until count) {
            addPoint(ev.getHistoricalX(i), ev.getHistoricalY(i), ev.getHistoricalEventTime(i))
        }
        addPoint(ev.x, ev.y, time)
    }

    private fun addPoint(x: Float, y: Float, time: Long) {
        val buffer = mBuffer
        while (buffer.size() > 0) {
            val lastT = buffer.getTime(0)
            if (lastT >= time - LONGEST_PAST_TIME)
                break
            buffer.dropOldest()
        }
        buffer.add(x, y, time)
    }

    /**
     * Computes the current velocity from the movement history buffer.
     * Velocity is calculated as an exponential moving average of instantaneous
     * velocities between each recorded point. Results are available via
     * [getXVelocity] and [getYVelocity].
     *
     * @param units the multiplier for velocity (e.g., pixels per time unit)
     */
    fun computeCurrentVelocity(units: Int) {
        computeCurrentVelocity(units, Float.MAX_VALUE)
    }

    /**
     * Computes the current velocity from the movement history buffer with
     * a maximum velocity constraint.
     * Velocity is calculated as an exponential moving average of instantaneous
     * velocities between each recorded point, clamped to the specified maximum.
     *
     * @param units the multiplier for velocity (e.g., pixels per time unit)
     * @param maxVelocity the maximum allowed velocity magnitude
     */
    fun computeCurrentVelocity(units: Int, maxVelocity: Float) {
        val buffer = mBuffer
        val oldestX = buffer.getX(0)
        val oldestY = buffer.getY(0)
        val oldestTime = buffer.getTime(0)

        var accumX = 0f
        var accumY = 0f
        val count = buffer.size()
        for (pos in 1 until count) {
            val dur = (buffer.getTime(pos) - oldestTime).toInt()
            if (dur == 0) continue
            var dist = buffer.getX(pos) - oldestX
            var vel = (dist / dur) * units   // pixels/frame
            if (accumX == 0f) accumX = vel
            else accumX = (accumX + vel) * 0.5f

            dist = buffer.getY(pos) - oldestY
            vel = (dist / dur) * units   // pixels/frame
            if (accumY == 0f) accumY = vel
            else accumY = (accumY + vel) * 0.5f
        }
        mXVelocity = if (accumX < 0.0f) max(accumX, -maxVelocity)
        else min(accumX, maxVelocity)
        mYVelocity = if (accumY < 0.0f) max(accumY, -maxVelocity)
        else min(accumY, maxVelocity)
    }

    /**
     * Returns the X-axis velocity computed from the recent movement history.
     * Valid only after calling [computeCurrentVelocity] or
     * [computeCurrentVelocity].
     *
     * @return the X velocity in pixels per unit
     */
    fun getXVelocity(): Float {
        return mXVelocity
    }

    /**
     * Returns the Y-axis velocity computed from the recent movement history.
     * Valid only after calling [computeCurrentVelocity] or
     * [computeCurrentVelocity].
     *
     * @return the Y velocity in pixels per unit
     */
    fun getYVelocity(): Float {
        return mYVelocity
    }

    /**
     * A circular ring buffer that stores recent motion events (position and timestamp).
     * Maintains up to [SwipeTracker.NUM_PAST] events, automatically discarding
     * the oldest event when the buffer reaches capacity. Provides efficient access to
     * events by relative index where 0 is the oldest event.
     */
    internal class EventRingBuffer(private val bufSize: Int) {
        private val xBuf = FloatArray(bufSize)
        private val yBuf = FloatArray(bufSize)
        private val timeBuf = LongArray(bufSize)
        private var top = 0  // points new event
        private var end = 0  // points oldest event
        private var count = 0 // the number of valid data

        fun clear() {
            end = 0
            top = end
            count = top
        }

        fun size(): Int {
            return count
        }

        // Position 0 points oldest event
        private fun index(pos: Int): Int {
            return (end + pos) % bufSize
        }

        private fun advance(index: Int): Int {
            return (index + 1) % bufSize
        }

        fun add(x: Float, y: Float, time: Long) {
            xBuf[top] = x
            yBuf[top] = y
            timeBuf[top] = time
            top = advance(top)
            if (count < bufSize) {
                count++
            } else {
                end = advance(end)
            }
        }

        fun getX(pos: Int): Float {
            return xBuf[index(pos)]
        }

        fun getY(pos: Int): Float {
            return yBuf[index(pos)]
        }

        fun getTime(pos: Int): Long {
            return timeBuf[index(pos)]
        }

        fun dropOldest() {
            count--
            end = advance(end)
        }
    }

    companion object {
        private const val NUM_PAST = 4
        private const val LONGEST_PAST_TIME = 200
    }
}
