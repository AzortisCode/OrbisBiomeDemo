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
import com.azortis.orbis.biomedemo.objects.layer.BiomeLayer;
import com.azortis.orbis.biomedemo.objects.layer.Context;
import com.azortis.orbis.biomedemo.objects.layer.Layer;
import com.azortis.orbis.biomedemo.objects.layer.RegionLayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class BiomePointGatherer {

    private final Dimension dimension;
    private final int chunkWidth;

    public BiomePointGatherer(Dimension dimension, int chunkWidth){
        this.dimension = dimension;
        this.chunkWidth = chunkWidth;
    }

    public int getBiomeAt(double x, double z){
        final OpenSimplex2S noise = new OpenSimplex2S(dimension.getTypeSeed());
        final int chunkX = (int) (Math.round(x) >> 4);
        final int chunkZ = (int) (Math.round(z) >> 4);

        final Map<String, Double> contexts = new HashMap<>();

        ChunkPointGatherer<Double> pointGatherer = new ChunkPointGatherer<>(1.0 / dimension.getTypeZoom(), 32, chunkWidth);
        List<GatheredPoint<Double>> pointList = pointGatherer.getPointsFromChunkBase(dimension.getTypeSeed(), chunkX, chunkZ);
        final GatheredPoint<Double> closestTypePoint = getClosestPoint(x, z, pointList);
        assert closestTypePoint != null;
        final double typeNoise = Math.round(noise.noise(closestTypePoint.getX() / dimension.getTypeZoom(), closestTypePoint.getZ() / dimension.getTypeZoom()) *
                dimension.getPrecision()) / dimension.getPrecision();
        int type = -1;
        double typeContext = 0;
        if (typeNoise >= dimension.getLandMin() && typeNoise <= dimension.getLandMax()) {
            type = 0;
            typeContext = getContext(dimension.getLandMin(), dimension.getLandMax(), typeNoise);
        } else if (typeNoise >= dimension.getShoreMin() && typeNoise <= dimension.getShoreMax()) {
            type = 1;
            typeContext = getContext(dimension.getShoreMin(), dimension.getShoreMax(), typeNoise);
        } else if (typeNoise >= dimension.getSeaMin() && typeNoise <= dimension.getSeaMax()) {
            type = 2;
            typeContext = getContext(dimension.getSeaMin(), dimension.getSeaMax(), typeNoise);
        }
        contexts.put("type", typeContext);

        // Refresh to use for getting initial regions points
        pointGatherer = new ChunkPointGatherer<>(1.0 / dimension.getRegionZoom(), 32, chunkWidth);
        pointList = pointGatherer.getPointsFromChunkBase(dimension.getRegionSeed(), chunkX, chunkZ);

        GatheredPoint<Double> closestPoint = getClosestPoint(x, z, pointList); // Todo grab new closestPoint per region iteration, requires frequency and maxPointContribution to be variable
        noise.setSeed(dimension.getRegionSeed());
        double currentNoise = Math.round(noise.noise(closestPoint.getX() / dimension.getRegionZoom(), closestPoint.getZ() / dimension.getRegionZoom()) *
                dimension.getPrecision()) / dimension.getPrecision();
        Biome selectedBiome = null;
        Region currentRegion = null;

        for (RegionLayer layer : dimension.getRegions()){
            if(layer.getMin() <= currentNoise && layer.getMax() >= currentNoise){
                currentRegion = layer.getLayerObject();
                contexts.put(layer.getLayerName(), getContext(layer.getMin(), layer.getMax(), currentNoise));
            }
        }
        assert currentRegion != null;
        while (selectedBiome == null){

            // Calculate new noise
            noise.setSeed(currentRegion.getSeed());
            currentNoise = Math.round(noise.noise(closestPoint.getX() / currentRegion.getZoom(), closestPoint.getZ() / currentRegion.getZoom()) *
                    dimension.getPrecision()) / dimension.getPrecision();

            Layer<?> selectedLayer = null;
            double min = 0.0;
            double max = 0.0;

            if(type == 0){
                List<Layer<?>> layers = new ArrayList<>();
                layers.addAll(currentRegion.getLandRegions());
                layers.addAll(currentRegion.getLandBiomes());
                if (currentRegion.getContextSettings().isUseLandContext()) {
                    Map<double[], Layer<?>> layerMap = getLayerMap(layers, contexts);
                    double[] selectedMinMax = null;
                    for (double[] minMax : layerMap.keySet()) {
                        if (currentNoise >= minMax[0] && currentNoise <= minMax[1]) {
                            selectedLayer = layerMap.get(minMax);
                            selectedMinMax = minMax;
                        }
                    }
                    assert selectedLayer != null;
                    min = selectedMinMax[0];
                    max = selectedMinMax[1];
                } else {
                    selectedLayer = getLayer(layers, currentNoise);
                    assert selectedLayer != null;
                    min = selectedLayer.getMin();
                    max = selectedLayer.getMax();
                }
            } else if(type == 1){
                List<Layer<?>> layers = new ArrayList<>();
                layers.addAll(currentRegion.getShoreRegions());
                layers.addAll(currentRegion.getShoreBiomes());
                if (currentRegion.getContextSettings().isUseShoreContext()) {
                    Map<double[], Layer<?>> layerMap = getLayerMap(layers, contexts);
                    double[] selectedMinMax = null;
                    for (double[] minMax : layerMap.keySet()) {
                        if (currentNoise >= minMax[0] && currentNoise <= minMax[1]) {
                            selectedLayer = layerMap.get(minMax);
                            selectedMinMax = minMax;
                        }
                    }
                    assert selectedLayer != null;
                    min = selectedMinMax[0];
                    max = selectedMinMax[1];
                } else {
                    selectedLayer = getLayer(layers, currentNoise);
                    assert selectedLayer != null;
                    min = selectedLayer.getMin();
                    max = selectedLayer.getMax();
                }
            } else if (type == 2){
                List<Layer<?>> layers = new ArrayList<>();
                layers.addAll(currentRegion.getSeaRegions());
                layers.addAll(currentRegion.getSeaBiomes());
                if (currentRegion.getContextSettings().isUseSeaContext()) {
                    Map<double[], Layer<?>> layerMap = getLayerMap(layers, contexts);
                    double[] selectedMinMax = null;
                    for (double[] minMax : layerMap.keySet()) {
                        if (currentNoise >= minMax[0] && currentNoise <= minMax[1]) {
                            selectedLayer = layerMap.get(minMax);
                            selectedMinMax = minMax;
                        }
                    }
                    assert selectedLayer != null;
                    min = selectedMinMax[0];
                    max = selectedMinMax[1];
                } else {
                    selectedLayer = getLayer(layers, currentNoise);
                    assert selectedLayer != null;
                    min = selectedLayer.getMin();
                    max = selectedLayer.getMax();
                }
            }

            if (selectedLayer instanceof RegionLayer) {
                // Will trigger another loop
                currentRegion = ((RegionLayer) selectedLayer).getLayerObject();
                contexts.put(selectedLayer.getLayerName(), getContext(min, max, currentNoise));
            } else if (selectedLayer instanceof BiomeLayer) {
                // Will end the while loop
                selectedBiome = ((BiomeLayer) selectedLayer).getLayerObject();
            }
        }
        return selectedBiome.getId();
    }

    private GatheredPoint<Double> getClosestPoint(double x, double z, List<GatheredPoint<Double>> points){
        GatheredPoint<Double> closestPoint = null; // Is the point of which the distance squared is the shortest
        for (GatheredPoint<Double> point : points){
            double dx = x - point.getX();
            double dz = z - point.getZ();
            point.setTag(dx * dx + dz * dz);

            if(closestPoint == null){
                closestPoint = point;
            } else if(point.getTag() < closestPoint.getTag()){
                closestPoint = point;
            }
        }
        return closestPoint;
    }

    @NotNull
    private Map<double[], Layer<?>> getLayerMap(final List<Layer<?>> layers, final Map<String, Double> contexts) {
        List<Layer<?>> participatingLayers = new ArrayList<>();
        for (Layer<?> layer : layers) {
            boolean add = true;
            for (Context context : layer.getContexts()){
                double contextDouble = contexts.get(context.getContext());
                if(!(context.getMin() <= contextDouble && context.getMax() >= contextDouble))add = false;
            }
            if(add)participatingLayers.add(layer);
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

    @Nullable
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

    private double getContext(double min, double max, double noise) {
        final double range = max - min;
        final double value = noise - min;
        return Math.round((((value / range) * 2) - 1) * dimension.getPrecision()) / dimension.getPrecision();
    }

}
