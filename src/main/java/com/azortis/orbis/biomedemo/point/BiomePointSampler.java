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
    private final int typeContributionRadiusSq;

    public BiomePointSampler(Dimension dimension, int chunkWidth) {
        this.dimension = dimension;
        this.chunkWidth = chunkWidth;

        seed = dimension.getSeed();
        cellFrequency = 1.0 / dimension.getCellZoom();
        cellPointContributionRadius = dimension.getCellPointContributionRadius();
        typeContributionRadiusSq = dimension.getTypeContributionRadius() * dimension.getTypeContributionRadius();
    }

    public int getBiomeAt(double x, double z) {
        // Assign the noise with the type seed first, as we're going to calculate types first.
        final OpenSimplex2S noise = new OpenSimplex2S(dimension.getTypeSeed());

        // Get the base coordinates for this point.
        final int chunkX = (int) (Math.round(x) >> 4);
        final int chunkZ = (int) (Math.round(z) >> 4);

        // Gather all points that might have influence on our cell.
        final ChunkPointGatherer<PointEval> pointGatherer = new ChunkPointGatherer<>(cellFrequency, cellPointContributionRadius, chunkWidth);
        final List<GatheredPoint<PointEval>> allPoints = pointGatherer.getPointsFromChunkBase(seed, chunkX, chunkZ);
        assert allPoints.size() > 0;

        // First we're going to calculate the closestPoint, this is the cell we're actually going to search
        // search the biome for. The calculations for every other point is just to aid in getting this one.
        GatheredPoint<PointEval> currentClosestPoint = null;
        for (GatheredPoint<PointEval> point : allPoints) {
            // For optimization purposes we're going to locate the type for this point in this iteration loop

            // Assign a PointEval tag for each point.
            point.setTag(new PointEval());

            // Get the noise for the type, and assign the noise to the eval.
            final double typeNoise = Math.round(noise.noise(point.getX() / dimension.getTypeZoom(), point.getZ() / dimension.getTypeZoom()) *
                    dimension.getPrecision()) / dimension.getPrecision();
            point.getTag().typeNoise = typeNoise;

            // Assign a type based on the noise values configured in the Dimension file
            if (typeNoise >= dimension.getLandMin() && typeNoise <= dimension.getLandMax()) {
                point.getTag().type = 0;
            } else if (typeNoise >= dimension.getShoreMin() && typeNoise <= dimension.getShoreMax()) {
                point.getTag().type = 1;
            } else if (typeNoise >= dimension.getSeaMin() && typeNoise <= dimension.getSeaMax()) {
                point.getTag().type = 2;
            }

            // Calculate and assign the distance squared to the x,z coordinates using a pythagoras distance function
            final double dx = point.getX() - x;
            final double dz = point.getZ() - z;
            point.getTag().distanceSquared = dx * dx + dz * dz;

            // Check if the distance squared is lower than the currentClosestPoint, if so assign this point.
            // And assign this point if current is null, to initialize the value.
            if (currentClosestPoint == null) {
                currentClosestPoint = point;
            } else if (currentClosestPoint.getTag().distanceSquared > point.getTag().distanceSquared) {
                currentClosestPoint = point;
            }
        }

        // Assign the value as final
        final GatheredPoint<PointEval> closestPoint = currentClosestPoint;

        // Create a list of all points that need to be calculated, will shrink the closer we get to the biome
        List<GatheredPoint<PointEval>> pointsToSearch = new ArrayList<>(allPoints);
        pointsToSearch.remove(closestPoint);
        pointsToSearch.removeIf(point -> point.getTag().type != closestPoint.getTag().type);

        // Calculate & assign the initial region for closest point.
        final List<Layer<?>> initialRegions = new ArrayList<>(dimension.getRegions());
        final double initialRegionNoise = Math.round(noise.noise(closestPoint.getX() / dimension.getRegionZoom(),
                closestPoint.getZ() / dimension.getRegionZoom()) * dimension.getPrecision()) / dimension.getPrecision();
        final Layer<?> initialRegionLayer = getLayer(initialRegions, initialRegionNoise);

        closestPoint.getTag().layers.add(new LayerEval(initialRegionLayer, initialRegionNoise,
                initialRegionLayer.getMin(), initialRegionLayer.getMax()));

        // Now we have to calculate the initial regions for all our points that have the same type.
        // Assign the region seed to the noise generator
        noise.setSeed(dimension.getRegionSeed());

        // Iterate all points and assign region layer
        final List<GatheredPoint<PointEval>> pointsToRemove = new ArrayList<>();
        for (GatheredPoint<PointEval> point : pointsToSearch) {
            // Get the noise for the region
            final double regionNoise = Math.round(noise.noise(point.getX() / dimension.getRegionZoom(), point.getZ() / dimension.getRegionZoom()) *
                    dimension.getPrecision()) / dimension.getPrecision();

            // Get the region layer
            final Layer<?> region = getLayer(initialRegions, regionNoise);

            if (region == initialRegionLayer) {
                // Create a new LayerEval and assign it to position 0 in the ArrayList
                point.getTag().layers.add(new LayerEval(region, regionNoise, region.getMin(), region.getMax()));
            } else {
                // Remove from points to search, as this point now has become irrelevant
                pointsToRemove.add(point);
            }
        }
        pointsToSearch.removeAll(pointsToRemove);
        pointsToRemove.clear();

        // Calculate and assign the type strength context for each point with the same type & region.
        pointsToSearch.add(closestPoint);
        for (GatheredPoint<PointEval> point : pointsToSearch){
            point.getTag().contexts.put("type", getTypeStrength(point, allPoints));
            point.getTag().contexts.put(initialRegionLayer.getLayerName(),
                    getLayerStrength(point, 0, dimension.getRegionContributionRadius(), allPoints));
        }
        pointsToSearch.remove(closestPoint);

        int iteration = 1;
        Biome selectedBiome = null;

        while (selectedBiome == null){
            final Region region = (Region) closestPoint.getTag().layers.get(iteration - 1).layer.getLayerObject();
            noise.setSeed(region.getSeed());

            // Get the layers for this iteration
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

            // Calculate the region/biome for closestPoint
            final double closestLayerNoise = Math.round(noise.noise(closestPoint.getX() / region.getZoom(), closestPoint.getZ() / region.getZoom()) *
                    dimension.getPrecision()) / dimension.getPrecision();
            final LayerEval closestLayer = getLayer(layers, useContext, closestLayerNoise, closestPoint.getTag().contexts);

            if(closestLayer.layer.getLayerObject() instanceof Region){
                closestPoint.getTag().layers.add(closestLayer);
                for (GatheredPoint<PointEval> point : pointsToSearch){
                    final double layerNoise = Math.round(noise.noise(point.getX() / region.getZoom(), point.getZ() / region.getZoom()) *
                            dimension.getPrecision()) / dimension.getPrecision();
                    final LayerEval layer = getLayer(layers, useContext, layerNoise, point.getTag().contexts);

                    if(layer.layer == closestLayer.layer){
                        point.getTag().layers.add(layer);
                    } else {
                        pointsToRemove.add(point);
                    }
                }
                pointsToSearch.removeAll(pointsToRemove);
                pointsToRemove.clear();

                pointsToSearch.add(closestPoint);
                for (GatheredPoint<PointEval> point : pointsToSearch){
                    double layerStrength = getLayerStrength(point, iteration, region.getContributionRadius(), allPoints);
                    point.getTag().contexts.put(closestLayer.layer.getLayerName(), layerStrength);
                }
                pointsToSearch.remove(closestPoint);
                iteration++;
            } else {
                selectedBiome = (Biome) closestLayer.layer.getLayerObject();
            }
        }
        return selectedBiome.getId();
    }

    private double getLayerStrength(GatheredPoint<PointEval> point, int iteration, int contributionRadius,
                                    List<GatheredPoint<PointEval>> points){
        // Get the layer evaluation of the point at iteration
        LayerEval layerEval = point.getTag().layers.get(iteration);

        // The current closest distance squared with another layer
        double closestDistanceSq = Double.MAX_VALUE;

        // First we're going to iterate through all points, and calculate the closest distance to point with another layer.
        for (GatheredPoint<PointEval> point1 : points){
            if(point1.getTag().layers.size() >= iteration + 1) {
                LayerEval layerEval1 = point1.getTag().layers.get(iteration);
                if (layerEval1.layer != layerEval.layer) {
                    // Calculate the squared distance using pythagoras
                    final double dx = point.getX() - point1.getX();
                    final double dz = point.getZ() - point1.getZ();
                    final double distanceSq = dx * dx + dz * dz;

                    // Check if the squared distance is lower than the current squared distance. If so assign this distance.
                    if (distanceSq < closestDistanceSq) closestDistanceSq = distanceSq;
                }
            }
        }

        // Calculate initial layer strength with no coefficient applied.
        double layerStrength = getStrength(layerEval.min, layerEval.max, layerEval.layerNoise);

        // If closest distance is in the contribution radius, then apply the coefficient to the strength.
        int contributionRadiusSq = contributionRadius * contributionRadius;
        if(closestDistanceSq < contributionRadiusSq){
            layerStrength *= (closestDistanceSq / contributionRadiusSq);

            // Make sure to round it again to the amount of decimal places * 10 configured in Dimension
            layerStrength = Math.round(layerStrength * dimension.getPrecision()) / dimension.getPrecision();
        }
        return layerStrength;
    }

    private double getTypeStrength(GatheredPoint<PointEval> point, List<GatheredPoint<PointEval>> points){
        // The current closest distance squared to point with another type.
        double closestDistanceSq = Double.MAX_VALUE;

        // First we're going to iterate through all the points, and calculate the closest distance to point with another type.
        for (GatheredPoint<PointEval> point1 : points){
            if(point1.getTag().type != point.getTag().type) {
                // Calculate the squared distance using pythagoras
                final double dx = point.getX() - point1.getX();
                final double dz = point.getZ() - point1.getZ();
                final double distanceSq = dx * dx + dz * dz;

                // Check if the squared distance is lower than the current squared distance. If so assign this distance.
                if (distanceSq < closestDistanceSq) closestDistanceSq = distanceSq;
            }
        }

        // Get the corresponding min and max values of the type.
        double min = -1.0d;
        double max = 1.0d;
        switch (point.getTag().type){
            case 0:
                min = dimension.getLandMin();
                max = dimension.getLandMax();
                break;
            case 1:
                min = dimension.getShoreMin();
                max = dimension.getShoreMax();
                break;
            case 2:
                min = dimension.getSeaMin();
                max = dimension.getSeaMax();
        }

        // Calculate the strength purely based on the noise map.
        double typeStrength = getStrength(min, max, point.getTag().typeNoise);

        // If the closest distance is in the type contribution radius, then apply the coefficient to the strength.
        if(closestDistanceSq < typeContributionRadiusSq) {
            typeStrength *= (closestDistanceSq / typeContributionRadiusSq);

            // Make sure to round it again to the amount of decimal places * 10 configured in Dimension
            typeStrength = Math.round(typeStrength * dimension.getPrecision()) / dimension.getPrecision();
        }
        return typeStrength;
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

    private LayerEval getLayer(@NotNull List<Layer<?>> layers, boolean useContext, double layerNoise, Map<String, Double> contexts) {
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
        return new LayerEval(selectedLayer, layerNoise, min, max);
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

    private static class PointEval {
        double distanceSquared;
        int type;
        double typeNoise;
        Map<String, Double> contexts = new HashMap<>();
        List<LayerEval> layers = new ArrayList<>();
    }

    private static class LayerEval {
        final Layer<?> layer;
        final double layerNoise;
        double min;
        double max;

        public LayerEval(Layer<?> layer, double layerNoise, double min, double max) {
            this.layer = layer;
            this.layerNoise = layerNoise;
            this.min = min;
            this.max = max;
        }
    }

}
