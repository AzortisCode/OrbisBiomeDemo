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

package com.azortis.orbis.biomedemo.objects;

import com.azortis.orbis.biomedemo.objects.layer.BiomeLayer;
import com.azortis.orbis.biomedemo.objects.layer.RegionLayer;

import java.util.List;

/**
 * A region can have biomes or more regions that it represents.
 *
 * If this specified region is only
 */
public class Region {

    private String name;

    // Use context
    private ContextSettings contextSettings;

    // The seed for the noise algorithm
    private long seed;

    // The zoom
    private double zoom;

    private List<RegionLayer> landRegions;
    private List<BiomeLayer> landBiomes;

    private List<RegionLayer> shoreRegions;
    private List<BiomeLayer> shoreBiomes;

    private List<RegionLayer> seaRegions;
    private List<BiomeLayer> seaBiomes;

    private int contributionRadius;

    public Region(){
    }

    public String getName() {
        return name;
    }

    public ContextSettings getContextSettings() {
        return contextSettings;
    }

    public long getSeed() {
        return seed;
    }

    public double getZoom() {
        return zoom;
    }

    public List<RegionLayer> getLandRegions() {
        return landRegions;
    }

    public List<BiomeLayer> getLandBiomes() {
        return landBiomes;
    }

    public List<RegionLayer> getShoreRegions() {
        return shoreRegions;
    }

    public List<BiomeLayer> getShoreBiomes() {
        return shoreBiomes;
    }

    public List<RegionLayer> getSeaRegions() {
        return seaRegions;
    }

    public List<BiomeLayer> getSeaBiomes() {
        return seaBiomes;
    }

    public int getContributionRadius() {
        return contributionRadius;
    }
}
