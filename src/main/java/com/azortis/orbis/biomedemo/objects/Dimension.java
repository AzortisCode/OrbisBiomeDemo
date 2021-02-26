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

import com.azortis.orbis.biomedemo.objects.layer.RegionLayer;

import java.util.List;

public class Dimension {

    private String name;

    // Seed used for the point gatherer etc
    private long seed;

    // The amount of decimal places times 2 to take into account when calculation layers, all configuration files must be made according to this(So no missing decimal places!)
    private long precision;

    private long regionSeed;
    private int regionZoom;

    // The type is global to make sure sea's etc are connected via shores
    private long typeSeed;
    private int typeZoom;

    // The min's and maxes for the type noise map, to apply globally
    private double seaMin;
    private double seaMax;
    private double shoreMin;
    private double shoreMax;
    private double landMin;
    private double landMax;

    private double cellZoom;
    private int cellPointContributionRadius;
    private int typeContributionRadius; // Must be lower than above
    private int regionContributionRadius;

    // Initial regions to sample from, should mostly represent temperature
    //
    // The best order is: Climate -> Sub-climates -> Biome zone(Variations of the same biome like Pine forrest has multiple types) -> Actual biomes.
    private List<RegionLayer> regions;

    public Dimension() {
    }

    public String getName() {
        return name;
    }

    public long getSeed() {
        return seed;
    }

    public double getPrecision() {
        return precision;
    }

    public long getRegionSeed() {
        return regionSeed;
    }

    public int getRegionZoom() {
        return regionZoom;
    }

    public long getTypeSeed() {
        return typeSeed;
    }

    public int getTypeZoom() {
        return typeZoom;
    }

    public double getSeaMin() {
        return seaMin;
    }

    public double getSeaMax() {
        return seaMax;
    }

    public double getShoreMin() {
        return shoreMin;
    }

    public double getShoreMax() {
        return shoreMax;
    }

    public double getLandMin() {
        return landMin;
    }

    public double getLandMax() {
        return landMax;
    }

    public double getCellZoom() {
        return cellZoom;
    }

    public int getCellPointContributionRadius() {
        return cellPointContributionRadius;
    }

    public int getTypeContributionRadius() {
        return typeContributionRadius;
    }

    public int getRegionContributionRadius() {
        return regionContributionRadius;
    }

    public List<RegionLayer> getRegions() {
        return regions;
    }

}
