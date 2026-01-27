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

package org.pocketworkstation.pckeyboard;

import org.pocketworkstation.pckeyboard.Keyboard.Key;

import java.util.Arrays;

/**
 * Key detector implementation for the main keyboard with proximity detection capability.
 *
 * <p>This detector identifies the key pressed based on touch coordinates and detects nearby keys
 * within a configurable proximity threshold. It can detect up to 12 nearby keys and ranks them
 * by distance from the touch point.
 *
 * <p>The proximity detection is useful for correcting common touch inaccuracies on touchscreen
 * keyboards, allowing the IME to suggest alternative keys when the user may have slightly missed
 * their intended target. Keys are ranked by their squared distance from the touch point, with
 * the closest key returned as the primary result and others available as alternatives.
 *
 * <p>Proximity detection workflow:
 * <ul>
 *   <li>Obtains the set of nearest keys to the touch point from the keyboard layout</li>
 *   <li>Checks each nearby key to determine if it's directly touched or within proximity threshold</li>
 *   <li>Calculates squared distance for performance optimization</li>
 *   <li>Maintains a distance-ranked array of nearby key codes for autocorrection engine</li>
 *   <li>Returns the primary key index and populates alternative nearby codes if requested</li>
 * </ul>
 *
 * @see KeyDetector
 * @see Keyboard.Key
 */
public class ProximityKeyDetector extends KeyDetector {
    private static final int MAX_NEARBY_KEYS = 12;

    // working area
    private int[] mDistances = new int[MAX_NEARBY_KEYS];

    /**
     * Provides the maximum number of nearby keys that can be detected by this proximity detector.
     *
     * @return The maximum number of nearby keys (12)
     */
    @Override
    public int getMaxNearbyKeys() {
        return MAX_NEARBY_KEYS;
    }

    /**
     * Detects the primary key at the given touch coordinates and identifies nearby keys.
     *
     * <p>This method implements the core proximity detection logic:
     * <ul>
     *   <li>Identifies the key directly under the touch point (if any)</li>
     *   <li>Scans all nearby keys within the proximity threshold distance</li>
     *   <li>Ranks nearby keys by squared distance from the touch point</li>
     *   <li>Stores proximity-ranked key codes in the allKeys array for autocorrection</li>
     * </ul>
     *
     * <p>The detection uses squared distance calculations for performance and compares against
     * {@code mProximityThresholdSquare}. Only keys with character codes > 32 (printable characters)
     * are considered for proximity detection. The method handles multiple keys at the same distance
     * by inserting all their codes in order.
     *
     * @param x The x-coordinate of the touch point
     * @param y The y-coordinate of the touch point
     * @param allKeys An array to store the codes of nearby keys ranked by distance, or null if
     *                nearby codes are not needed. The array is populated in order of increasing
     *                distance from the touch point.
     * @return The index of the primary key (the key directly under the touch point, or the closest
     *         key within proximity threshold if no key is directly touched). Returns
     *         {@code LatinKeyboardBaseView.NOT_A_KEY} if no valid key is detected.
     */
    @Override
    public int getKeyIndexAndNearbyCodes(int x, int y, int[] allKeys) {
        final Key[] keys = getKeys();
        final int touchX = getTouchX(x);
        final int touchY = getTouchY(y);
        int primaryIndex = LatinKeyboardBaseView.NOT_A_KEY;
        int closestKey = LatinKeyboardBaseView.NOT_A_KEY;
        int closestKeyDist = mProximityThresholdSquare + 1;
        int[] distances = mDistances;
        Arrays.fill(distances, Integer.MAX_VALUE);
        int [] nearestKeyIndices = mKeyboard.getNearestKeys(touchX, touchY);
        final int keyCount = nearestKeyIndices.length;
        for (int i = 0; i < keyCount; i++) {
            final Key key = keys[nearestKeyIndices[i]];
            int dist = 0;
            boolean isInside = key.isInside(touchX, touchY);
            if (isInside) {
                primaryIndex = nearestKeyIndices[i];
            }

            if (((mProximityCorrectOn
                    && (dist = key.squaredDistanceFrom(touchX, touchY)) < mProximityThresholdSquare)
                    || isInside)
                    && key.codes[0] > 32) {
                // Find insertion point
                final int nCodes = key.codes.length;
                if (dist < closestKeyDist) {
                    closestKeyDist = dist;
                    closestKey = nearestKeyIndices[i];
                }

                if (allKeys == null) continue;

                for (int j = 0; j < distances.length; j++) {
                    if (distances[j] > dist) {
                        // Make space for nCodes codes
                        System.arraycopy(distances, j, distances, j + nCodes,
                                distances.length - j - nCodes);
                        System.arraycopy(allKeys, j, allKeys, j + nCodes,
                                allKeys.length - j - nCodes);
                        System.arraycopy(key.codes, 0, allKeys, j, nCodes);
                        Arrays.fill(distances, j, j + nCodes, dist);
                        break;
                    }
                }
            }
        }
        if (primaryIndex == LatinKeyboardBaseView.NOT_A_KEY) {
            primaryIndex = closestKey;
        }
        return primaryIndex;
    }
}
