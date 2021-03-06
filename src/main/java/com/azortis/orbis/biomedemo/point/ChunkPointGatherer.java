/*
 * MIT License
 *
 * Copyright (c) 2021 Azortis
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.azortis.orbis.biomedemo.point;

import java.util.List;

public class ChunkPointGatherer<TTag> {

    private static final double CHUNK_RADIUS_RATIO = Math.sqrt(1.0 / 2.0);

    private final int halfChunkWidth;
    private final double maxPointContributionRadius;
    private final double maxPointContributionRadiusSq;
    double radiusPlusHalfChunkWidth;
    private final UnfilteredPointGatherer<TTag> unfilteredPointGatherer;

    public ChunkPointGatherer(double frequency, double maxPointContributionRadius, int chunkWidth) {
        this.halfChunkWidth = chunkWidth / 2;
        this.maxPointContributionRadius = maxPointContributionRadius;
        this.maxPointContributionRadiusSq = maxPointContributionRadius * maxPointContributionRadius;
        this.radiusPlusHalfChunkWidth = maxPointContributionRadius + halfChunkWidth;
        unfilteredPointGatherer = new UnfilteredPointGatherer<>(frequency,
                maxPointContributionRadius + chunkWidth * CHUNK_RADIUS_RATIO);
    }

    public List<GatheredPoint<TTag>> getPointsFromChunkBase(long seed, int chunkBaseWorldX, int chunkBaseWorldZ) {
        return getPointsFromChunkCenter(seed, chunkBaseWorldX + halfChunkWidth, chunkBaseWorldZ + halfChunkWidth);
    }

    public List<GatheredPoint<TTag>> getPointsFromChunkCenter(long seed, int chunkCenterWorldX, int chunkCenterWorldZ) {
        List<GatheredPoint<TTag>> worldPoints =
                unfilteredPointGatherer.getPoints(seed, chunkCenterWorldX, chunkCenterWorldZ);
        for (int i = 0; i < worldPoints.size(); i++) {
            GatheredPoint<TTag> point = worldPoints.get(i);

            // Check if point contribution radius lies outside chunk
            double axisCheckValueX = Math.abs(point.getX() - chunkCenterWorldX) - halfChunkWidth;
            double axisCheckValueZ = Math.abs(point.getZ() - chunkCenterWorldZ) - halfChunkWidth;
            if (axisCheckValueX >= maxPointContributionRadius || axisCheckValueZ >= maxPointContributionRadius
                    || (axisCheckValueX > 0 && axisCheckValueZ > 0
                    && axisCheckValueX * axisCheckValueX + axisCheckValueZ * axisCheckValueZ >= maxPointContributionRadiusSq)) {

                // If so, remove it.
                // Copy the last value to this value, and remove the last,
                // to avoid shifting and because order doesn't matter.
                int lastIndex = worldPoints.size() - 1;
                worldPoints.set(i, worldPoints.get(lastIndex));
                worldPoints.remove(lastIndex);
            }
        }

        return worldPoints;
    }

}