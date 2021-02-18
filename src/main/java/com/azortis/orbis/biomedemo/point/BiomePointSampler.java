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

import com.azortis.orbis.biomedemo.noise.OpenSimplex2S;
import com.azortis.orbis.biomedemo.objects.Biome;
import com.azortis.orbis.biomedemo.objects.Dimension;
import com.azortis.orbis.biomedemo.objects.Region;
import com.azortis.orbis.biomedemo.objects.layer.Context;
import com.azortis.orbis.biomedemo.objects.layer.Layer;
import com.azortis.orbis.biomedemo.objects.layer.RegionLayer;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class BiomePointSampler {

    private final static double MIN_NOISE = -1.0d;
    private final static double MAX_NOISE = 1.0d;

    private final Dimension dimension;
    private final int chunkWidth;

    private final long seed;
    private final double cellFrequency;
    private final int cellPointContributionRadius;
    private final int typeContributionRadiusSquared;
    private final int regionContributionRadiusSquared;

    public BiomePointSampler(Dimension dimension, int chunkWidth) {
        this.dimension = dimension;
        this.chunkWidth = chunkWidth;

        seed = dimension.getSeed();
        cellFrequency = 1.0 / dimension.getCellZoom();
        cellPointContributionRadius = dimension.getCellPointContributionRadius();
        typeContributionRadiusSquared = dimension.getMaxTypeContributionRadius() * dimension.getMaxTypeContributionRadius();
        regionContributionRadiusSquared = dimension.getMaxRegionContributionRadius() * dimension.getMaxRegionContributionRadius();
    }

    public int getBiomeAt(double x, double z) {
        final OpenSimplex2S noise = new OpenSimplex2S(dimension.getTypeSeed());
        final int chunkX = (int) (Math.round(x) >> 4);
        final int chunkZ = (int) (Math.round(z) >> 4);

        final Map<String, Double> contexts = new HashMap<>();
        final ChunkPointGatherer<PointEvaluation> pointGatherer = new ChunkPointGatherer<>(cellFrequency, cellPointContributionRadius, chunkWidth);
        final List<GatheredPoint<PointEvaluation>> gatheredPoints = pointGatherer.getPointsFromChunkBase(seed, chunkX, chunkZ);
        GatheredPoint<PointEvaluation> closestPoint = null; // The cell we're currently in.

        for (GatheredPoint<PointEvaluation> point : gatheredPoints) {
            final double typeNoise = Math.round(noise.noise(point.getX() / dimension.getTypeZoom(), point.getZ() / dimension.getTypeZoom()) *
                    dimension.getPrecision()) / dimension.getPrecision();
            point.setTag(new PointEvaluation());
            point.getTag().typeNoise = typeNoise;
            if (typeNoise >= dimension.getLandMin() && typeNoise <= dimension.getLandMax()) {
                point.getTag().type = 0;
            } else if (typeNoise >= dimension.getShoreMin() && typeNoise <= dimension.getShoreMax()) {
                point.getTag().type = 1;
            } else if (typeNoise >= dimension.getSeaMin() && typeNoise <= dimension.getSeaMax()) {
                point.getTag().type = 2;
            }
            final double dx = point.getX() - x;
            final double dz = point.getZ() - z;
            point.getTag().distanceSquared = dx * dx + dz * dz; // Pythagoras

            if (closestPoint == null) closestPoint = point;
            else if (closestPoint.getTag().distanceSquared > point.getTag().distanceSquared) closestPoint = point;
        }
        assert closestPoint != null;
        GatheredPoint<PointEvaluation> closestOtherTypePoint = null;
        for (GatheredPoint<PointEvaluation> point : gatheredPoints) {
            if (point != closestPoint) {
                final double dx = point.getX() - closestPoint.getX();
                final double dz = point.getZ() - closestPoint.getZ();
                point.getTag().distanceSquared2Closest = dx * dx + dz * dz;

                if (point.getTag().type != closestPoint.getTag().type) {
                    if (closestOtherTypePoint == null) closestOtherTypePoint = point;
                    else if (closestOtherTypePoint.getTag().distanceSquared2Closest >
                            point.getTag().distanceSquared2Closest) closestOtherTypePoint = point;
                }
            }
        }

        final double typeNoise = closestPoint.getTag().typeNoise;
        double typeStrength = 0.0d;
        switch (closestPoint.getTag().type) {
            case 0:
                typeStrength = getStrength(dimension.getLandMin(), dimension.getLandMax(), typeNoise);
                break;
            case 1:
                typeStrength = getStrength(dimension.getShoreMin(), dimension.getShoreMax(), typeNoise);
                break;
            case 2:
                typeStrength = getStrength(dimension.getSeaMin(), dimension.getSeaMax(), typeNoise);
        }

        if (closestOtherTypePoint != null) {
            if (closestOtherTypePoint.getTag().distanceSquared2Closest <= typeContributionRadiusSquared) {
                typeStrength *= (closestOtherTypePoint.getTag().distanceSquared2Closest / typeContributionRadiusSquared);
                typeStrength = Math.round(typeStrength * dimension.getPrecision()) / dimension.getPrecision();
                if (typeStrength < 0) typeStrength = 0.0d;
                if (typeStrength > 1) typeStrength = 0.0d;
            }
        }
        contexts.put("type", typeStrength);

        final double initialRegionNoise = Math.round(noise.noise(closestPoint.getX() / dimension.getRegionZoom(), closestPoint.getZ() /
                dimension.getRegionZoom()) * dimension.getPrecision()) / dimension.getPrecision();
        RegionLayer currentRegionLayer = null;
        for (RegionLayer layer : dimension.getRegions()) {
            if (layer.getMin() <= initialRegionNoise && layer.getMax() >= initialRegionNoise) {
                currentRegionLayer = layer;
            }
        }
        assert currentRegionLayer != null;
        GatheredPoint<PointEvaluation> closestOtherRegionPoint = null;
        for (GatheredPoint<PointEvaluation> point : gatheredPoints) {
            final double regionNoise = Math.round(noise.noise(point.getX() / dimension.getRegionZoom(), point.getZ() /
                    dimension.getRegionZoom()) * dimension.getPrecision()) / dimension.getPrecision();
            RegionLayer regionLayer = null;
            for (RegionLayer layer : dimension.getRegions()) {
                if (layer.getMin() <= regionNoise && layer.getMax() >= regionNoise) {
                    regionLayer = layer;
                }
            }
            assert regionLayer != null;
            point.getTag().regionLayers.add(regionLayer);

            if (regionLayer != currentRegionLayer && point != closestPoint &&
                    point.getTag().distanceSquared2Closest <= regionContributionRadiusSquared) {
                if (closestOtherRegionPoint == null) closestOtherRegionPoint = point;
                else if (point.getTag().distanceSquared2Closest < closestOtherRegionPoint.getTag().distanceSquared2Closest)
                    closestOtherRegionPoint = point;
            }
        }

        double initialRegionStrength = getStrength(currentRegionLayer.getMin(), currentRegionLayer.getMax(), initialRegionNoise);
        if (closestOtherRegionPoint != null) {
            initialRegionStrength *= (closestOtherRegionPoint.getTag().distanceSquared2Closest / regionContributionRadiusSquared);
            initialRegionStrength = Math.round(initialRegionStrength * dimension.getPrecision()) / dimension.getPrecision();
            if (initialRegionStrength < 0) initialRegionStrength = 0.0d;
            if (initialRegionStrength > 1) initialRegionStrength = 1.0d;
        }

        contexts.put(currentRegionLayer.getLayerName(), initialRegionStrength);
        Biome selectedBiome = null;

        int iteration = 1;

        while (selectedBiome == null) {

            // Step one, calculate the currentPoint
            final Region region = currentRegionLayer.getLayerObject();
            final double layerNoise = Math.round(noise.noise(closestPoint.getX() / region.getZoom(), closestPoint.getZ() /
                    region.getZoom()) * dimension.getPrecision()) / dimension.getPrecision();

            List<Layer<?>> layers = new ArrayList<>();
            boolean useContext = false;
            switch (closestPoint.getTag().type) {
                case 0:
                    layers.addAll(region.getLandRegions());
                    layers.addAll(region.getLandBiomes());
                    useContext = region.getContextSettings().isUseLandContext();
                    break;
                case 1:
                    layers.addAll(region.getShoreRegions());
                    layers.addAll(region.getShoreBiomes());
                    useContext = region.getContextSettings().isUseShoreContext();
                    break;
                case 2:
                    layers.addAll(region.getSeaRegions());
                    layers.addAll(region.getSeaBiomes());
                    useContext = region.getContextSettings().isUseSeaContext();
            }

            Object[] layerEval = getLayer(layers, useContext, layerNoise, contexts);
            Layer<?> selectedLayer = (Layer<?>) layerEval[2];
            double min = (double) layerEval[0];
            double max = (double) layerEval[1];

            double layerStrength = getStrength(min, max, layerNoise);

            // Iterate through all relevant points to get the closest, so we can modify our above strength.
            GatheredPoint<PointEvaluation> closestOtherLayerPoint = null;

            for (GatheredPoint<PointEvaluation> point : gatheredPoints) {
                if (point != closestPoint && point.getTag().distanceSquared2Closest < region.getMaxContributionRadius()) {
                    // For optimization purposes we can make a few assumptions luckily :0

                    if (point.getTag().type != closestPoint.getTag().type) {
                        if (closestOtherLayerPoint == null) closestOtherLayerPoint = point;
                        else if (closestOtherLayerPoint.getTag().distanceSquared2Closest >
                                point.getTag().distanceSquared2Closest) closestOtherLayerPoint = point;
                        continue;
                    }

                    // All you need to know if the last known layer is not equal to the current layer
                    boolean isOtherLayer = false;
                    for (int i = 1; i <= iteration; i++) {
                        if (point.getTag().regionLayers.get(i) != closestPoint.getTag().regionLayers.get(i)) {
                            isOtherLayer = true;
                            break;
                        }
                    }

                    if(isOtherLayer){
                        if (closestOtherLayerPoint == null) closestOtherLayerPoint = point;
                        else if (closestOtherLayerPoint.getTag().distanceSquared2Closest >
                                point.getTag().distanceSquared2Closest) closestOtherLayerPoint = point;
                    } else {
                        final double pointNoise = Math.round(noise.noise(point.getX() / region.getZoom(), point.getZ() /
                                region.getZoom()) * dimension.getPrecision()) / dimension.getPrecision();

                    }

                }
            }

        }

        return selectedBiome.getId();
    }

    private double getStrength(double min, double max, double value) {
        if (value < min || value > max) return 0;
        if (min == MIN_NOISE && max == MAX_NOISE) {
            value += 1.0d;
        } else if (max == MAX_NOISE) {
            value = getContext(min, max, value) + 1.0d;
        } else if (min == MIN_NOISE) {
            value = Math.abs(getContext(min, max, value) - 1.0d);
        } else {
            double median = Math.round((((min + 1.0d) + (max + 1.0d)) / 2.0d) * dimension.getPrecision())
                    / dimension.getPrecision();
            value = getContext(min, max, value) + 1.0d;
            if (value == median) {
                value = 2.0d;
            } else {
                // Coords: A(0;2) & B(medianOffset;0)
                // Slope = 2 / medianOffset
                // Final function: f(x) = -slope * x + 2
                double medianOffset = max - median;
                double slope = 2.0 / medianOffset;
                double x = Math.abs(value - median);
                value = -slope * x + 2.0d;
            }
        }
        return Math.round((value / 2.0d) * dimension.getPrecision()) / dimension.getPrecision();
    }

    private double getContext(double min, double max, double value) {
        double range = max - min;
        value = value - min;
        return Math.round((((value / range) * 2) - 1) * dimension.getPrecision()) / dimension.getPrecision();
    }

    private Object[] getLayer(List<Layer<?>> layers, boolean useContext, double layerNoise, Map<String, Double> contexts){
        Layer<?> selectedLayer = null;
        double min;
        double max;
        if (useContext) {
            Map<double[], Layer<?>> layerMap = getLayerMap(layers, contexts);
            double[] selectedMinMax = null;
            for (double[] minMax : layerMap.keySet()) {
                if (layerNoise >= minMax[0] && layerNoise <= minMax[1]) {
                    selectedLayer = layerMap.get(minMax);
                    selectedMinMax = minMax;
                }
            }
            assert selectedLayer != null;
            min = selectedMinMax[0];
            max = selectedMinMax[1];
        } else {
            selectedLayer = getLayer(layers, layerNoise);
            min = selectedLayer.getMin();
            max = selectedLayer.getMax();
        }
        return new Object[]{min, max, selectedLayer};
    }

    @NotNull
    private Map<double[], Layer<?>> getLayerMap(final List<Layer<?>> layers, final Map<String, Double> contexts) {
        List<Layer<?>> participatingLayers = new ArrayList<>();
        for (Layer<?> layer : layers) {
            boolean add = true;
            for (Context context : layer.getContexts()) {
                double contextDouble = contexts.get(context.getContext());
                if (!(context.getMin() <= contextDouble && context.getMax() >= contextDouble)) add = false;
            }
            if (add) participatingLayers.add(layer);
        }
        participatingLayers.sort(Comparator.comparingInt(Layer::getIndex));
        int maxChance = layers.stream().mapToInt(Layer::getChance).sum();
        double chancePerTicket = Math.round((2.0d / maxChance) * dimension.getPrecision()) / dimension.getPrecision();
        Map<double[], Layer<?>> layerMap = new HashMap<>();
        double currentMin = -1;
        for (Layer<?> layer : layers) {
            double min = currentMin;
            double max = min + layer.getChance() * chancePerTicket;
            layerMap.put(new double[]{min, max}, layer);
            currentMin = max + 1.0 / dimension.getPrecision();
        }
        return layerMap;
    }

    private Layer<?> getLayer(final List<Layer<?>> layers, final double noise) {
        Layer<?> layerResult = null;
        for (Layer<?> layer : layers) {
            if (noise >= layer.getMin() && noise <= layer.getMax()) {
                layerResult = layer;
                break;
            }
        }
        return layerResult;
    }

    private static class PointEvaluation {
        double distanceSquared;
        double distanceSquared2Closest;
        int type = -1;
        double typeNoise = 0;
        List<RegionLayer> regionLayers = new ArrayList<>(); // Less to store ;)
        Map<String, Double> contexts = new HashMap<>();
    }

}
