/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.systemui.monet;


import android.annotation.ColorInt;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.graphics.ColorUtils;


/**
 * Generate sets of colors that are shades of the same color
 */
@VisibleForTesting
public class Shades {
    /**
     *  Combining the ability to convert between relative luminance and perceptual luminance with
     *  contrast leads to a design system that can be based on a linear value to determine contrast,
     *  rather than a ratio.
     *
     *  This codebase implements a design system that has that property, and as a result, we can
     *  guarantee that any shades 5 steps from each other have a contrast ratio of at least 4.5.
     *  4.5 is the requirement for smaller text contrast in WCAG 2.1 and earlier.
     *
     *  However, lstar 50 does _not_ have a contrast ratio >= 4.5 with lstar 100.
     *  lstar 49.6 is the smallest lstar that will lead to a contrast ratio >= 4.5 with lstar 100,
     *  and it also contrasts >= 4.5 with lstar 100.
     */
    public static final float MIDDLE_LSTAR = 49.6f;

    /**
     * Generate shades of a color. Ordered in lightness _descending_.
     * <p>
     * The first shade will be at 95% lightness, the next at 90, 80, etc. through 0.
     *
     * @param hue    hue in CAM16 color space
     * @param chroma chroma in CAM16 color space
     * @param lFactor multiplier of luminance
     * @param cFactor multiplier of chroma
     * @return shades of a color, as argb integers. Ordered by lightness descending.
     */
    public static @ColorInt int[] of(float hue, float chroma, float lFactor, float cFactor) {
        int[] shades = new int[12];
        // At tone 90 and above, blue and yellow hues can reach a much higher chroma.
        // To preserve a consistent appearance across all hues, use a maximum chroma of 40.
        shades[0] = ColorUtils.CAMToColor(hue, Math.min(40f, chroma) * cFactor, 99 * lFactor);
        shades[1] = ColorUtils.CAMToColor(hue, Math.min(40f, chroma) * cFactor, 95 * lFactor);
        for (int i = 2; i < 12; i++) {
            float lStar = (i == 6) ? MIDDLE_LSTAR : 100 - 10 * (i - 1);
            float shadeChroma = (lStar >= 90) ? Math.min(40f, chroma  * cFactor) : chroma * cFactor;
            shades[i] = ColorUtils.CAMToColor(hue, shadeChroma, lStar * lFactor);
        }
        return shades;
    }

    public static @ColorInt int[] of(float hue, float chroma) {
        return of(hue, chroma, 1f);
    }

    public static @ColorInt int[] of(float hue, float chroma, float lFactor) {
        return of(hue, chroma, lFactor, 1f);
    }
}
