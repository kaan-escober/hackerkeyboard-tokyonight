package org.pocketworkstation.pckeyboard.graphics

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable

/**
 * Custom drawable that renders a keyboard popup with smooth, seamless curves connecting
 * the trigger key to the popup menu.
 *
 * This drawable implements a sophisticated path rendering algorithm that creates visually
 * appealing S-curves between the key and popup using dynamic radius scaling. When space is
 * limited, the corner radii are automatically reduced to maintain smooth transitions while
 * fitting within tight geometric constraints.
 *
 * Key Features:
 * - Dynamic radius scaling for tight spaces
 * - Smooth S-curve connections on both left and right sides
 * - Configurable corner radii for popup and key elements
 * - Snap logic to align edges when nearly aligned (within corner radius distance)
 * - Support for custom colors and stroke widths
 *
 * The drawing algorithm constructs a path that:
 * 1. Starts at the bottom-left corner of the key
 * 2. Curves up the left side, transitioning from key to popup
 * 3. Traces the popup outline with rounded corners
 * 4. Curves down the right side, transitioning from popup back to key
 * 5. Traces the bottom of the key with rounded corners and closes
 *
 * @author Hacker's Keyboard
 */
class SeamlessPopupDrawable(context: Context) : Drawable() {

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private val path = Path()
    private val keyRect = Rect()
    private val popupRect = Rect()

    private var keyHeight = 0f
    private var popupHeight = 0f
    private var strokeWidth = 0f
    private var cornerRadius = 0f
    private var keyCornerRadius = 0f
    private var neckHeight = 0f
    private var keyTopY = 0f
    private var popupTopY = 0f

    /**
     * Sets the background and stroke colors for the popup drawable.
     *
     * @param backgroundColor The fill color for the popup and key shapes
     * @param strokeColor The outline color for the stroke
     */
    fun setColors(backgroundColor: Int, strokeColor: Int) {
        backgroundPaint.color = backgroundColor
        strokePaint.color = strokeColor
    }

    /**
     * Sets the width of the stroke that outlines the popup and key shapes.
     * A stroke width of 0 will result in no outline being drawn.
     *
     * @param strokeWidth The width of the stroke in pixels
     */
    fun setStrokeWidth(strokeWidth: Float) {
        this.strokeWidth = strokeWidth
        strokePaint.strokeWidth = strokeWidth
    }

    /**
     * Sets the corner radius for the popup shape. This radius is used for the four corners
     * of the popup rectangle and influences the S-curve connections to the key.
     *
     * @param cornerRadius The radius of the corners in pixels
     */
    fun setCornerRadius(cornerRadius: Float) {
        this.cornerRadius = cornerRadius
    }

    /**
     * Sets the corner radius for the key shape. This radius is used for the bottom-left
     * and bottom-right corners of the key rectangle that is part of the drawn path.
     *
     * @param keyCornerRadius The radius of the key corners in pixels
     */
    fun setKeyCornerRadius(keyCornerRadius: Float) {
        this.keyCornerRadius = keyCornerRadius
    }

    /**
     * Sets the geometric parameters that define the position and size of the key and popup.
     * This method must be called by LatinKeyboardBaseView whenever the popup geometry changes
     * (e.g., when the user presses a key or the keyboard layout changes).
     *
     * The geometry parameters define the bounding rectangles and vertical positions that are
     * used by the updatePath() method to construct the smooth connecting path.
     *
     * @param keyRect The bounding rectangle of the trigger key
     * @param popupRect The bounding rectangle of the popup content
     * @param keyHeight The height of the key
     * @param popupHeight The height of the popup
     * @param neckHeight The height of the connecting "neck" region between key and popup
     * @param keyTopY The Y coordinate of the top of the key
     * @param popupTopY The Y coordinate of the top of the popup
     */
    fun setGeometry(
        keyRect: Rect,
        popupRect: Rect,
        keyHeight: Float,
        popupHeight: Float,
        neckHeight: Float,
        keyTopY: Float,
        popupTopY: Float
    ) {
        this.keyRect.set(keyRect)
        this.popupRect.set(popupRect)
        this.keyHeight = keyHeight
        this.popupHeight = popupHeight
        this.neckHeight = neckHeight
        this.keyTopY = keyTopY
        this.popupTopY = popupTopY

        updatePath()
        invalidateSelf() // Request redraw
    }

    /**
     * Internal method that reconstructs the path connecting the key and popup shapes.
     *
     * This method implements the core path rendering algorithm:
     * - Applies snap logic to align edges when they're within corner radius distance
     * - Constructs smooth S-curves on left and right sides using arc operations
     * - Dynamically scales curve radii when space is tight to maintain smooth transitions
     * - Traces the popup outline with rounded corners
     * - Traces the key outline with rounded corners
     * - Closes the path to create a complete shape
     *
     * The algorithm handles several geometric cases:
     * - Standard S-curves when there's sufficient space
     * - Dynamic radius scaling for tight spaces
     * - Straight transitions when edges are aligned or hanging out
     * - Snap tolerance to ensure crisp vertical lines when virtually aligned
     */
    private fun updatePath() {
        path.reset()

        // Inset by half the stroke width to avoid clipping, as STROKE draws centered on the path.
        val halfStroke = strokeWidth / 2f

        // Small snap tolerance to ensure crisp vertical lines when virtually aligned
        val snapDistance = strokeWidth

        var keyLeft = keyRect.left + halfStroke
        var keyRight = keyRect.right - halfStroke
        val keyBottom = keyRect.bottom - halfStroke
        val keyTop = keyTopY

        var popupLeft = popupRect.left + halfStroke
        var popupRight = popupRect.right - halfStroke
        val popupTop = popupRect.top + halfStroke

        // Apply Snap Logic
        if (kotlin.math.abs(keyLeft - popupLeft) <= cornerRadius) {
            popupLeft = keyLeft
        }
        if (kotlin.math.abs(keyRight - popupRight) <= cornerRadius) {
            popupRight = keyRight
        }

        val filletRadius = keyCornerRadius

        // --- Start Drawing ---

        // 1. Start at Key Left Edge (Just above bottom corner)
        path.moveTo(keyLeft, keyBottom - keyCornerRadius)

        // 2. Left Side Connection
        val leftDelta = keyLeft - popupLeft

        if (leftDelta > 0) {
            // Key is inside Popup (Key Right of Popup Left)
            // We need to go Left to reach Popup

            val availableSpace = leftDelta
            val requiredSpace = cornerRadius + filletRadius

            if (availableSpace >= requiredSpace) {
                // Standard S-Curve
                path.lineTo(keyLeft, keyTop + filletRadius)
                // Turn Left (CCW) onto shelf
                path.arcTo(RectF(keyLeft - 2 * filletRadius, keyTop, keyLeft, keyTop + 2 * filletRadius), 0f, -90f)
                path.lineTo(popupLeft + cornerRadius, keyTop)
                // Turn Right (CW) onto popup
                path.arcTo(RectF(popupLeft, keyTop - 2 * cornerRadius, popupLeft + 2 * cornerRadius, keyTop), 90f, 90f)
            } else {
                // Tight Space - Dynamic Radius Scaling
                // Split the available delta between the two curves
                val dynamicRadius = availableSpace / 2f

                path.lineTo(keyLeft, keyTop + dynamicRadius)
                // Turn Left (CCW)
                path.arcTo(RectF(keyLeft - 2 * dynamicRadius, keyTop, keyLeft, keyTop + 2 * dynamicRadius), 0f, -90f)
                // No lineTo needed, we are at the midpoint
                // Turn Right (CW)
                path.arcTo(RectF(popupLeft, keyTop - 2 * dynamicRadius, popupLeft + 2 * dynamicRadius, keyTop), 90f, 90f)
            }
        } else {
            // Key is aligned or hanging out (Key Left of Popup Left)
            // Just go straight up
            path.lineTo(keyLeft, keyTop)
            if (keyLeft != popupLeft) path.lineTo(popupLeft, keyTop)
            path.lineTo(popupLeft, popupTop + cornerRadius)
        }

        // 3. Popup Top-Left Corner
        path.arcTo(RectF(popupLeft, popupTop, popupLeft + 2 * cornerRadius, popupTop + 2 * cornerRadius), 180f, 90f)

        // 4. Popup Top Edge & Top-Right Corner
        path.lineTo(popupRight - cornerRadius, popupTop)
        path.arcTo(RectF(popupRight - 2 * cornerRadius, popupTop, popupRight, popupTop + 2 * cornerRadius), 270f, 90f)

        // 5. Right Side Connection
        val rightDelta = popupRight - keyRight

        if (rightDelta > 0) {
            // Key is inside Popup (Key Left of Popup Right)
            // We need to go Left to reach Key

            val availableSpace = rightDelta
            val requiredSpace = cornerRadius + filletRadius

            if (availableSpace >= requiredSpace) {
                // Standard S-Curve
                path.lineTo(popupRight, keyTop - cornerRadius)
                // Turn Right (CW) onto shelf
                path.arcTo(RectF(popupRight - 2 * cornerRadius, keyTop - 2 * cornerRadius, popupRight, keyTop), 0f, 90f)
                path.lineTo(keyRight + filletRadius, keyTop)
                // Turn Left (CCW) onto key
                path.arcTo(RectF(keyRight, keyTop, keyRight + 2 * filletRadius, keyTop + 2 * filletRadius), 270f, -90f)
            } else {
                // Tight Space - Dynamic Radius Scaling
                val dynamicRadius = availableSpace / 2f

                path.lineTo(popupRight, keyTop - dynamicRadius)
                // Turn Right (CW)
                path.arcTo(RectF(popupRight - 2 * dynamicRadius, keyTop - 2 * dynamicRadius, popupRight, keyTop), 0f, 90f)
                // Turn Left (CCW)
                path.arcTo(RectF(keyRight, keyTop, keyRight + 2 * dynamicRadius, keyTop + 2 * dynamicRadius), 270f, -90f)
            }
            path.lineTo(keyRight, keyBottom - keyCornerRadius)
        } else {
            // Aligned or hanging out
            path.lineTo(popupRight, keyTopY)
            if (keyRight != popupRight) path.lineTo(keyRight, keyTopY)
            path.lineTo(keyRight, keyBottom - keyCornerRadius)
        }

        // 6. Key Bottom-Right Corner
        path.arcTo(RectF(keyRight - 2 * keyCornerRadius, keyBottom - 2 * keyCornerRadius, keyRight, keyBottom), 0f, 90f)

        // 7. Key Bottom Edge
        path.lineTo(keyLeft + keyCornerRadius, keyBottom)

        // 8. Key Bottom-Left Corner
        path.arcTo(RectF(keyLeft, keyBottom - 2 * keyCornerRadius, keyLeft + 2 * keyCornerRadius, keyBottom), 90f, 90f)

        path.close()
    }

    /**
     * Renders the popup drawable onto the provided canvas.
     *
     * This method draws both the background fill and the stroke outline of the connected
     * key-popup shape. The stroke is only drawn if the stroke width has been set to a
     * value greater than 0.
     *
     * @param canvas The canvas on which to draw the popup drawable
     */
    override fun draw(canvas: Canvas) {
        // Draw the background
        canvas.drawPath(path, backgroundPaint)

        // Draw the stroke (only if stroke width is greater than 0)
        if (strokeWidth > 0) {
            canvas.drawPath(path, strokePaint)
        }
    }

    override fun setAlpha(alpha: Int) {
        // Not typically used for drawables that directly control their own alpha/color
    }

    override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {
        // Not typically used for drawables that directly control their own alpha/color
    }

    @Deprecated("Deprecated in Java", ReplaceWith("PixelFormat.OPAQUE", "android.graphics.PixelFormat"))
    override fun getOpacity(): Int {
        // OPAQUE is generally safe if the drawable always fills its bounds
        return PixelFormat.OPAQUE
    }

    /**
     * Returns the combined bounding rectangle of both the key and popup shapes.
     *
     * This method calculates the union of the key and popup rectangles, which represents
     * the full bounds of the drawable. This is useful for invalidation and layout calculations.
     *
     * @return A rectangle that encompasses both the key and popup shapes
     */
    fun getCombinedBounds(): Rect {
        val combinedBounds = Rect(keyRect)
        combinedBounds.union(popupRect)
        return combinedBounds
    }
}
