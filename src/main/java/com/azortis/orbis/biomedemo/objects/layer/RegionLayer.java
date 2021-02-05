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

package com.azortis.orbis.biomedemo.objects.layer;

import com.azortis.orbis.biomedemo.Registry;
import com.azortis.orbis.biomedemo.objects.Region;

public class RegionLayer implements Layer<Region> {

    private String regionName;

    // If not using contexts, then use the default distribution method
    private double min;
    private double max;

    // If providing contexts, the min/max system is not viable and thus first a pool is created using
    // the contexts and then it will use the noise to spread the participating regions/biomes
    // along that map, upping the chance will give it a bigger range
    private int index;
    private int chance;
    private Context parentContext;
    private Context typeContext;

    public String getRegionName() {
        return regionName;
    }

    @Override
    public Region getLayerObject() {
        return Registry.getRegion(regionName);
    }

    @Override
    public double getMin() {
        return min;
    }

    @Override
    public double getMax() {
        return max;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public int getChance() {
        return 0;
    }

    @Override
    public Context getParentContext() {
        return parentContext;
    }

    @Override
    public Context getTypeContext() {
        return typeContext;
    }
}
