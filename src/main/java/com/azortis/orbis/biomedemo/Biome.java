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

import java.awt.*;

public enum Biome {
    HOT_DARKER_RED(1,0, 0, -1, -0.33, Color.RED.darker().darker()),
    HOT_DARK_RED(2,0, 0, -0.32, 0.33, Color.RED.darker()),
    HOT_RED(3,0, 0, 0.34, 1, Color.RED),
    HOT_LIGHT_YELLOW(4,0, 1, 0, 1, new Color(255, 255, 128)),
    HOT_LIGHT_BLUE(5,0, 2, 0, 1, new Color(0, 155, 255)),
    TEMPERATE_BLUE(6,1, 2, 0, 1, new Color(0, 100, 255)),
    TEMPERATE_YELLOW(7, 1, 1, 0, 1, new Color(255, 255, 51)),
    TEMPERATE_GREEN(8, 1, 0, -1, -0.33, Color.GREEN),
    TEMPERATE_DARK_GREEN(9, 1, 0, -0.32, 0.33, Color.GREEN.darker()),
    TEMPERATE_DARKER_GREEN(10, 1, 0 , 0.34, 1, Color.GREEN.darker().darker()),
    COLD_DARK_BLUE(11, 2, 2, 0, 1, new Color(0, 38, 255)),
    COLD_DARK_YELLOW(12, 2, 1, 0, 1, new Color(255, 255, 0)),
    COLD_MAGENTA(13, 2, 0, -1, -0.33, Color.MAGENTA),
    COLD_DARK_MAGENTA(14, 2, 0, -0.32, 0.33, Color.MAGENTA.darker()),
    COLD_DARKER_MAGENTA(15, 2, 0, 0.34, 1, Color.MAGENTA.darker().darker());


    private final int biomeId; // The index id for the BiomeBlender to recognize.
    private final int region; // 0 = HOT, 1 = TEMPERATE & 2 = COLD
    private final int type; // 0 = LAND, 1 = SHORE & 2 = SEA
    private final double biomeMin; // The minimum value on the noise map for this biome -- UNIQUE 2 decimals max!
    private final double biomeMax; // The maximum value on the noise map for this biome -- UNIQUE 2 decimals max!
    private final Color color; // Pretty obvious

    Biome(int biomeId, int region, int type, double biomeMin, double biomeMax, Color color) {
        this.biomeId = biomeId;
        this.region = region;
        this.type = type;
        this.biomeMin = biomeMin;
        this.biomeMax = biomeMax;
        this.color = color;
    }

    public int getBiomeId() {
        return biomeId;
    }

    public int getRegion() {
        return region;
    }

    public int getType() {
        return type;
    }

    public double getBiomeMin() {
        return biomeMin;
    }

    public double getBiomeMax() {
        return biomeMax;
    }

    public Color getColor() {
        return color;
    }

    public static Biome getBiome(int biomeId){
        switch (biomeId){
            case 1:
                return HOT_DARKER_RED;
            case 2:
                return HOT_DARK_RED;
            case 3:
                return HOT_RED;
            case 4:
                return HOT_LIGHT_YELLOW;
            case 5:
                return HOT_LIGHT_BLUE;
            case 6:
                return TEMPERATE_BLUE;
            case 7:
                return TEMPERATE_YELLOW;
            case 8:
                return TEMPERATE_GREEN;
            case 9:
                return TEMPERATE_DARK_GREEN;
            case 10:
                return TEMPERATE_DARKER_GREEN;
            case 11:
                return COLD_DARK_BLUE;
            case 12:
                return COLD_DARK_YELLOW;
            case 13:
                return COLD_MAGENTA;
            case 14:
                return COLD_DARK_MAGENTA;
            case 15:
                return COLD_DARKER_MAGENTA;
        }
        return null;
    }

    public static Biome getBiome(int region, int type, double value){
        double roundedValue = Math.round(value * 100) / 100.0;
        if(region == 0){
            if(type == 2)return HOT_LIGHT_BLUE;
            if(type == 1)return HOT_LIGHT_YELLOW;
            if(roundedValue <= -0.33)return HOT_DARKER_RED;
            if(roundedValue >= -0.32 && roundedValue <= 0.33)return HOT_DARK_RED;
            if(roundedValue >= 0.34)return HOT_RED;
        }else if(region == 1){
            if(type == 2)return TEMPERATE_BLUE;
            if(type == 1)return TEMPERATE_YELLOW;
            if(roundedValue <= -0.33)return TEMPERATE_GREEN;
            if(roundedValue >= -0.32 && roundedValue <= 0.33)return TEMPERATE_DARK_GREEN;
            if(roundedValue >= 0.34)return TEMPERATE_DARKER_GREEN;
        }else if(region == 2){
            if(type == 2)return COLD_DARK_BLUE;
            if(type == 1)return COLD_DARK_YELLOW;
            if(roundedValue <= -0.33)return COLD_MAGENTA;
            if(roundedValue >= -0.32 && roundedValue <= 0.33)return COLD_DARK_MAGENTA;
            if(roundedValue >= 0.34)return COLD_DARKER_MAGENTA;
        }
        return null;
    }

}
