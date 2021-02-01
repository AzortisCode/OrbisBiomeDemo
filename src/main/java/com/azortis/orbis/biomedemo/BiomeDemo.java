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

package com.azortis.orbis.biomedemo;

import com.azortis.orbis.biomedemo.noise.OpenSimplex2S;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public final class BiomeDemo {

    private static final int WIDTH = 4096;
    private static final int HEIGHT = 1536;
    private static final int CHUNK_WIDTH = 16;

    private static final long REGION_SEED = 1242411432511L;
    private static final long TYPE_SEED = 24214356315145L;
    private static final long SEED = 98028491293421L;

    private static final int MIN_BLEND_RADIUS = 32;
    private static final double POINT_FREQUENCY = 0.02;
    private static final double REGION_ZOOM = 2500;
    private static final double TYPE_ZOOM = 750;
    private static final double BIOME_ZOOM = 50;

    private static final double LAND_MIN = 0.31;
    private static final double LAND_MAX = 1;
    private static final double SHORE_MIN = 0.12;
    private static final double SHORE_MAX = 0.30;
    private static final double SEA_MIN = -1;
    private static final double SEA_MAX = 0.11;

    private static final OpenSimplex2S regionNoise = new OpenSimplex2S(REGION_SEED);
    private static final OpenSimplex2S typeNoise = new OpenSimplex2S(TYPE_SEED);
    private static final OpenSimplex2S biomeNoise = new OpenSimplex2S(SEED);

    public static void main(String[] args){
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);

        ScatteredBiomeBlender biomeBlender = new ScatteredBiomeBlender(POINT_FREQUENCY, MIN_BLEND_RADIUS, CHUNK_WIDTH);

        for (int zc = 0; zc < HEIGHT; zc += CHUNK_WIDTH){
            for(int xc = 0; xc < WIDTH; xc += CHUNK_WIDTH){

                LinkedBiomeWeightMap firstBiomeWeightMap = biomeBlender.getBlendForChunk(SEED, xc, zc, BiomeDemo::getBiomeAt);

                for (int zi = 0; zi < CHUNK_WIDTH; zi++) {
                    for (int xi = 0; xi < CHUNK_WIDTH; xi++) {
                        int z = zc + zi;
                        int x = xc + xi;

                        double r, g, b; r = g = b = 0;

                        for (LinkedBiomeWeightMap entry = firstBiomeWeightMap; entry != null; entry = entry.getNext()) {
                            double weight = entry.getWeights()[zi * CHUNK_WIDTH + xi];
                            int biome = entry.getBiome();
                            Biome biome1 = Biome.getBiome(biome);
                            assert biome1 != null;
                            Color color = biome1.getColor();
                            r += color.getRed() * weight;
                            g += color.getGreen() * weight;
                            b += color.getBlue() * weight;
                        }

                        int rgb = new Color((int)r, (int)g, (int)b).getRGB();
                        image.setRGB(x, z, rgb);
                    }
                }
            }
        }
        JFrame frame = new JFrame();
        JLabel imageLabel = new JLabel();
        imageLabel.setIcon(new ImageIcon(image));
        frame.add(imageLabel);
        frame.setTitle("Orbis Biome Demo");
        frame.pack();
        frame.setResizable(false);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    private static int getBiomeAt(double x, double z){
        // Get the region
        double regionValue = Math.round(regionNoise.noise(x / REGION_ZOOM, z / REGION_ZOOM) * 100) / 100.0;
        int region = -1;
        if(regionValue <= -0.33)region = 0;
        if(regionValue >= -0.32 && regionValue <= 0.33)region = 1;
        if(regionValue >= 0.34)region = 2;

        // Get the type
        double typeValue = Math.round(typeNoise.noise(x / TYPE_ZOOM, z / TYPE_ZOOM) * 100) / 100.0;
        int type = -1;
        if(typeValue >= LAND_MIN && typeValue <= LAND_MAX)type = 0;
        if(typeValue >= SHORE_MIN && typeValue <= SHORE_MAX)type = 1;
        if(typeValue >= SEA_MIN && typeValue <= SEA_MAX)type = 2;

        if(region == -1 || typeValue == -1) System.out.println("ERROR: Something went wrong with calculation type or region");

        double biomeValue = biomeNoise.noise(x / BIOME_ZOOM, z / BIOME_ZOOM);
        Biome biome = Biome.getBiome(region, type, biomeValue);
        assert biome != null;
        return biome.getBiomeId();
    }

}
