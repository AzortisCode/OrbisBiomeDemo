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

public class ChunkBiomePointSampler {

    private final static double MIN_NOISE = -1.0d;
    private final static double MAX_NOISE = 1.0d;

    private final Dimension dimension;
    private final int maxSearchRadius;

    private final List<GatheredPoint<BiomeEval>> chunkPoints;
    private final List<GatheredPoint<PointEval>> allPoints;


    public ChunkBiomePointSampler(Dimension dimension, double cellFrequency, int chunkWidth, int searchRadius, int chunkX, int chunkZ) {
        this.dimension = dimension;

        // Collect all the points that may be searched for biome evaluation.
        ChunkPointGatherer<BiomeEval> chunkPointGatherer = new ChunkPointGatherer<>(cellFrequency, searchRadius, chunkWidth);

        final int chunkCenterX = chunkX + chunkWidth / 2;
        final int chunkCenterZ = chunkZ + chunkWidth / 2;
        chunkPoints = chunkPointGatherer.getPointsFromChunkCenter(dimension.getSeed(), chunkCenterX, chunkCenterZ);

        // Determine the full search radius for the unfiltered point gatherer, by sampling the furthest point from chunk
        // Center in chunkPoints.

        double furthestDistanceSq = 0.0d;
        for (GatheredPoint<BiomeEval> point : chunkPoints){
            point.setTag(new BiomeEval());
            double dX = chunkCenterX - point.getX();
            double dZ = chunkCenterZ - point.getZ();
            double distanceSq = dX * dX + dZ * dZ;

            if(distanceSq > furthestDistanceSq)furthestDistanceSq = distanceSq;
        }

        // Only use square root once!
        maxSearchRadius = (int) Math.round(Math.sqrt(furthestDistanceSq));
        int maxCellRadius = dimension.getCellPointContributionRadius() + maxSearchRadius;

        // Get all points that are needed for properly sampling the points for the biome evaluation.
        UnfilteredPointGatherer<PointEval> pointGatherer = new UnfilteredPointGatherer<>(cellFrequency, maxCellRadius);
        allPoints = pointGatherer.getPoints(dimension.getSeed(), chunkCenterX, chunkCenterZ);
        allPoints.forEach(point -> point.setTag(new PointEval()));

        // Calculate the distance squared to chunkCenter
        for (GatheredPoint<PointEval> point : allPoints){
            double dX = point.getX() - chunkCenterX;
            double dZ = point.getZ() - chunkCenterZ;
            point.getTag().distanceSq = dX * dX + dZ * dZ;
        }

        // Create a link to the PointEval and BiomeEval point where necessary
        for (GatheredPoint<PointEval> point : allPoints){
            if(point.getTag().distanceSq <= furthestDistanceSq){
                for (GatheredPoint<BiomeEval> chunkPoint : chunkPoints){
                    if(chunkPoint.getX() == point.getX() && chunkPoint.getZ() == point.getZ()){
                        point.getTag().isChunkPoint = true;
                        point.getTag().chunkPoint = chunkPoint;
                        chunkPoint.getTag().evaluationPoint = point;
                    }
                }
            }
        }

        assignTypes();
        assignInitialRegions();

        boolean allBiomesEvaluated = false;
        int iteration = 1;
        while (!allBiomesEvaluated){
            List<Layer<?>> layersToCalculate = new ArrayList<>();
            for (GatheredPoint<BiomeEval> chunkPoint : chunkPoints){
                if(!chunkPoint.getTag().biomeFound){
                    Layer<?> currentLayer = chunkPoint.getTag().evaluationPoint.getTag().layers.get(iteration - 1).layer;
                    if(!layersToCalculate.contains(currentLayer))layersToCalculate.add(currentLayer);
                }
            }
            for (Layer<?> layer : layersToCalculate){
                calculateRegion(layer, iteration);
            }
            boolean allBiomesFound = true;
            for (GatheredPoint<BiomeEval> chunkPoint : chunkPoints){
                allBiomesFound = chunkPoint.getTag().biomeFound;
            }
            allBiomesEvaluated = allBiomesFound;
            iteration++;
        }

        // Clear from memory since not needed anymore :)
        allPoints.clear();
    }

    private void calculateRegion(Layer<?> regionLayer, int iteration){
        Region region = (Region) regionLayer.getLayerObject();
        final OpenSimplex2S noise = new OpenSimplex2S(region.getSeed());
        int maxRegionRadius = region.getContributionRadius() + maxSearchRadius;
        final int maxRegionRadiusSq = maxRegionRadius * maxRegionRadius;

        for (GatheredPoint<PointEval> point : allPoints){
            if(iteration == point.getTag().layers.size() && point.getTag().distanceSq <= maxRegionRadiusSq &&
                    point.getTag().layers.get(iteration - 1).layer == regionLayer){
                final double regionNoise = getNoiseRounded(noise, point.getX(), point.getZ(), region.getZoom());

                boolean useContext = false;
                List<Layer<?>> layers = new ArrayList<>();
                switch (point.getTag().type){
                    case 0:
                        useContext = region.getContextSettings().isUseLandContext();
                        layers.addAll(region.getLandRegions());
                        layers.addAll(region.getLandBiomes());
                        break;
                    case 1:
                        useContext = region.getContextSettings().isUseShoreContext();
                        layers.addAll(region.getShoreRegions());
                        layers.addAll(region.getShoreBiomes());
                        break;
                    case 2:
                        useContext = region.getContextSettings().isUseSeaContext();
                        layers.addAll(region.getSeaRegions());
                        layers.addAll(region.getSeaBiomes());
                }

                LayerEval layer = getLayer(layers, useContext, regionNoise, point.getTag().contexts);
                point.getTag().layers.add(layer);
                double layerStrength = getStrength(layer.min, layer.max, regionNoise);
                point.getTag().contexts.put(layer.layer.getLayerName(), layerStrength);
            }
        }

        final List<Layer<?>> allRegionLayers = new ArrayList<>();
        allRegionLayers.addAll(region.getLandRegions());
        allRegionLayers.addAll(region.getLandBiomes());
        allRegionLayers.addAll(region.getShoreRegions());
        allRegionLayers.addAll(region.getShoreBiomes());
        allRegionLayers.addAll(region.getSeaRegions());
        allRegionLayers.addAll(region.getSeaBiomes());

        final int maxRegionContributionSq = region.getContributionRadius() * region.getContributionRadius();
        for (GatheredPoint<BiomeEval> chunkPoint : chunkPoints) {
            if (!chunkPoint.getTag().biomeFound) {
                GatheredPoint<PointEval> point = chunkPoint.getTag().evaluationPoint;

                if (iteration < point.getTag().layers.size()) {
                    final Layer<?> layer = point.getTag().layers.get(iteration).layer;
                    if (allRegionLayers.contains(layer)) {
                        if (layer.getLayerObject() instanceof Biome) {
                            chunkPoint.getTag().biome = ((Biome) layer.getLayerObject()).getId();
                            chunkPoint.getTag().biomeFound = true;
                            continue;
                        }

                        double closestDistanceSq = Double.MAX_VALUE;
                        for (GatheredPoint<PointEval> point1 : allPoints) {
                            if (point1.getTag().distanceSq <= maxRegionRadiusSq && iteration < point1.getTag().layers.size()) {
                                Layer<?> pointLayer = point1.getTag().layers.get(iteration).layer;
                                if (allRegionLayers.contains(pointLayer) && pointLayer != layer) {
                                    double dX = point1.getX() - point.getX();
                                    double dZ = point1.getZ() - point.getZ();
                                    double distanceSq = dX * dX + dZ * dZ;

                                    if (distanceSq < closestDistanceSq) closestDistanceSq = distanceSq;
                                }
                            }
                        }

                        double layerStrength = point.getTag().contexts.get(layer.getLayerName()) * (closestDistanceSq / maxRegionContributionSq);
                        layerStrength = Math.round(Math.min(1.00d, layerStrength) * dimension.getPrecision()) / dimension.getPrecision();
                        point.getTag().contexts.replace(layer.getLayerName(), layerStrength);
                    }
                }
            }
        }
    }

    private void assignTypes(){
        final OpenSimplex2S noise = new OpenSimplex2S(dimension.getTypeSeed());
        int maxTypeRadius = dimension.getTypeContributionRadius() + maxSearchRadius;
        final int maxTypeRadiusSq = maxTypeRadius * maxTypeRadius;

        for (GatheredPoint<PointEval> point : allPoints){
            if(point.getTag().distanceSq <= maxTypeRadiusSq){
                final double typeNoise = getNoiseRounded(noise, point.getX(), point.getZ(), dimension.getTypeZoom());

                double min = -1.0;
                double max = 1.0;
                if (typeNoise >= dimension.getLandMin() && typeNoise <= dimension.getLandMax()) {
                    point.getTag().type = 0;
                    min = dimension.getLandMin();
                    max = dimension.getLandMax();
                } else if (typeNoise >= dimension.getShoreMin() && typeNoise <= dimension.getShoreMax()) {
                    point.getTag().type = 1;
                    min = dimension.getShoreMin();
                    max = dimension.getShoreMax();
                } else if (typeNoise >= dimension.getSeaMin() && typeNoise <= dimension.getSeaMax()) {
                    point.getTag().type = 2;
                    min = dimension.getSeaMin();
                    max = dimension.getSeaMax();
                }

                double typeStrength = getStrength(min, max, typeNoise);
                point.getTag().contexts.put("type", typeStrength);
            }
        }

        final int maxTypeContributionSq = dimension.getTypeContributionRadius() * dimension.getTypeContributionRadius();
        for (GatheredPoint<BiomeEval> chunkPoint : chunkPoints){
            GatheredPoint<PointEval> point = chunkPoint.getTag().evaluationPoint;

            double closestDistanceSq = Double.MAX_VALUE;
            for (GatheredPoint<PointEval> point1 : allPoints){
                if(point1.getTag().distanceSq <= maxTypeRadiusSq && point1.getTag().type != point.getTag().type){
                    double dX = point.getX() - point1.getX();
                    double dZ = point.getZ() - point1.getZ();
                    double distanceSq = dX * dX + dZ * dZ;

                    if(distanceSq < closestDistanceSq)closestDistanceSq = distanceSq;
                }
            }

            double typeStrength = point.getTag().contexts.get("type") * (closestDistanceSq / maxTypeContributionSq);
            typeStrength = Math.round(Math.min(1.00d, typeStrength) * dimension.getPrecision()) / dimension.getPrecision();
            point.getTag().contexts.replace("type", typeStrength);
        }
    }

    private void assignInitialRegions(){
        final OpenSimplex2S noise = new OpenSimplex2S(dimension.getRegionSeed());
        int maxRegionRadius = dimension.getRegionContributionRadius() + maxSearchRadius;
        final int maxRegionRadiusSq = maxRegionRadius * maxRegionRadius;
        final List<Layer<?>> regionLayers = new ArrayList<>(dimension.getRegions());

        for(GatheredPoint<PointEval> point : allPoints){
            if(point.getTag().distanceSq <= maxRegionRadiusSq){
                final double regionNoise = getNoiseRounded(noise, point.getX(), point.getZ(), dimension.getRegionZoom());
                final Layer<?> layer = getLayer(regionLayers, regionNoise);

                point.getTag().layers.add(new LayerEval(layer, regionNoise, layer.getMin(), layer.getMax()));
                double regionStrength = getStrength(layer.getMin(), layer.getMax(), regionNoise);
                point.getTag().contexts.put(layer.getLayerName(), regionStrength);
            }
        }

        final int maxRegionContributionSq = dimension.getRegionContributionRadius() * dimension.getRegionContributionRadius();
        for (GatheredPoint<BiomeEval> chunkPoint : chunkPoints){
            GatheredPoint<PointEval> point = chunkPoint.getTag().evaluationPoint;
            Layer<?> initialLayer = point.getTag().layers.get(0).layer;

            double closestDistanceSq = Double.MAX_VALUE;
            for (GatheredPoint<PointEval> point1 : allPoints){
                if(point1.getTag().distanceSq <= maxRegionRadiusSq && point1.getTag().layers.get(0).layer != initialLayer){
                    double dX = point.getX() - point1.getX();
                    double dZ = point.getZ() - point1.getZ();
                    double distanceSq = dX * dX + dZ * dZ;

                    if(distanceSq < closestDistanceSq)closestDistanceSq = distanceSq;
                }
            }

            double regionStrength = point.getTag().contexts.get(initialLayer.getLayerName()) *
                    (closestDistanceSq / maxRegionContributionSq);
            regionStrength = Math.round(Math.min(1.00d, regionStrength) * dimension.getPrecision()) / dimension.getPrecision();
            point.getTag().contexts.replace(initialLayer.getLayerName(), regionStrength);
        }

    }

    private double getNoiseRounded(OpenSimplex2S noise, double x, double z, int zoom){
        return Math.round(noise.noise(x / zoom, z / zoom) * dimension.getPrecision()) / dimension.getPrecision();
    }

    //
    // Strength/context methods
    //

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

    //
    //  Layer evaluation methods
    //

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
        double chancePerTicket = 2.0d / maxChance;
        List<MapEval> layerList = new ArrayList<>();
        double currentMin = -1;
        for (Layer<?> layer : layers) {
            double min = currentMin;
            double max = min + Math.round(layer.getChance() * chancePerTicket * dimension.getPrecision()) / dimension.getPrecision();
            layerList.add(new MapEval(min, max, layer));
            currentMin = max + 1.0 / dimension.getPrecision();
        }
        // Fix possible errors
        Map<double[], Layer<?>> layerMap = new HashMap<>();
        currentMin = -1;
        for (int i = 0; i < layerList.size(); i++) {
            MapEval eval = layerList.get(i);
            if(i == 0){
                layerMap.put(new double[]{eval.min, eval.max}, eval.layer);
                currentMin = eval.max + 1.0 / dimension.getPrecision();
                continue;
            } else if (i == layerList.size() - 1){
                eval.max = 1.0d;
            }
            if(currentMin != eval.min){
                layerMap.put(new double[]{currentMin, eval.max}, eval.layer);
                currentMin = eval.max + 1.0 / dimension.getPrecision();
            }
        }
        return layerMap;
    }

    private static class MapEval {
        double min;
        double max;
        Layer<?> layer;

        public MapEval(double min, double max, Layer<?> layer) {
            this.min = min;
            this.max = max;
            this.layer = layer;
        }
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

    //
    // Biome evaluation callback
    //

    public int getBiomeAt(double x, double z){
        GatheredPoint<BiomeEval> closestPoint = null;
        double closestDistanceSq = Double.MAX_VALUE;

        for (GatheredPoint<BiomeEval> point : chunkPoints){
            double dX = point.getX() - x;
            double dZ = point.getZ() - z;
            double distanceSq = dX * dX + dZ * dZ;

            if(distanceSq < closestDistanceSq){
                closestPoint = point;
                closestDistanceSq = distanceSq;
            }
        }
        assert closestPoint != null;
        return closestPoint.getTag().biome;
    }

    //
    // Tag classes
    //

    private static class BiomeEval {
        int biome = -1;
        boolean biomeFound = false;
        GatheredPoint<PointEval> evaluationPoint;
    }

    private static class PointEval {
        double distanceSq; // The distance from this point to chunkCenter squared
        int type;
        List<LayerEval> layers = new ArrayList<>();
        Map<String, Double> contexts = new HashMap<>();

        // BiomePoint link
        boolean isChunkPoint = false;
        GatheredPoint<BiomeEval> chunkPoint = null;
    }

    private static class LayerEval {
        final Layer<?> layer;
        final double noise;
        final double min;
        final double max;

        public LayerEval(Layer<?> layer, double noise, double min, double max) {
            this.layer = layer;
            this.noise = noise;
            this.min = min;
            this.max = max;
        }
    }

}
