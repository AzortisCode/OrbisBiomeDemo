package com.azortis.orbis.biomedemo;

import java.awt.*;

public enum Biome {
    HOT_DARK_RED(1,0, 0, -1, -0.33, Color.RED.darker()),
    HOT_RED(2,0, 0, -0.32, 0.33, Color.RED),
    HOT_LIGHT_RED(3,0, 0, 0.34, 1, Color.RED.brighter()),
    HOT_LIGHTER_RED(4,0, 1, 0, 1, Color.RED.brighter().brighter()),
    HOT_LIGHT_BLUE(5,0, 2, 0, 1, Color.BLUE.brighter()),
    TEMPERATE_BLUE(6,1, 2, 0, 1, Color.BLUE),
    TEMPERATE_LIGHTER_GREEN(7, 1, 1, 0, 1, Color.GREEN.brighter().brighter()),
    TEMPERATE_LIGHT_GREEN(8, 1, 0, -1, -0.33, Color.GREEN.brighter()),
    TEMPERATE_GREEN(9, 1, 0, -0.32, 0.33, Color.GREEN),
    TEMPERATE_DARK_GREEN(10, 1, 0 , 0.34, 1, Color.GREEN.darker()),
    COLD_DARK_BLUE(11, 2, 2, 0, 1, Color.BLUE.darker()),
    COLD_LIGHTER_MAGENTA(12, 2, 1, 0, 1, Color.MAGENTA.brighter().brighter()),
    COLD_LIGHT_MAGENTA(13, 2, 0, -1, -0.33, Color.MAGENTA.brighter()),
    COLD_MAGENTA(14, 2, 0, -0.32, 0.33, Color.MAGENTA),
    COLD_DARK_MAGENTA(15, 2, 0, 0.34, 1, Color.MAGENTA.darker());


    private final int biomeId;
    private final int region; // 0 = HOT, 1 = TEMPERATE & 2 = COLD
    private final int type; // 0 = LAND, 1 = SHORE & 2 = SEA
    private final double biomeMin;
    private final double biomeMax;
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
                return HOT_DARK_RED;
            case 2:
                return HOT_RED;
            case 3:
                return HOT_LIGHT_RED;
            case 4:
                return HOT_LIGHTER_RED;
            case 5:
                return HOT_LIGHT_BLUE;
            case 6:
                return TEMPERATE_BLUE;
            case 7:
                return TEMPERATE_LIGHTER_GREEN;
            case 8:
                return TEMPERATE_LIGHT_GREEN;
            case 9:
                return TEMPERATE_GREEN;
            case 10:
                return TEMPERATE_DARK_GREEN;
            case 11:
                return COLD_DARK_BLUE;
            case 12:
                return COLD_LIGHTER_MAGENTA;
            case 13:
                return COLD_LIGHT_MAGENTA;
            case 14:
                return COLD_MAGENTA;
            case 15:
                return COLD_DARK_MAGENTA;
        }
        return null;
    }

    public static Biome getBiome(int region, int type, double value){
        double roundedValue = Math.round(value * 100) / 100.0;
        if(region == 0){
            if(type == 2)return HOT_LIGHT_BLUE;
            if(type == 1)return HOT_LIGHTER_RED;
            if(roundedValue <= -0.33)return HOT_DARK_RED;
            if(roundedValue >= -0.32 && roundedValue <= 0.33)return HOT_RED;
            if(roundedValue >= 0.34)return HOT_LIGHT_RED;
        }else if(region == 1){
            if(type == 2)return TEMPERATE_BLUE;
            if(type == 1)return TEMPERATE_LIGHTER_GREEN;
            if(roundedValue <= -0.33)return TEMPERATE_LIGHT_GREEN;
            if(roundedValue >= -0.32 && roundedValue <= 0.33)return TEMPERATE_GREEN;
            if(roundedValue >= 0.34)return TEMPERATE_DARK_GREEN;
        }else if(region == 2){
            if(type == 2)return COLD_DARK_BLUE;
            if(type == 1)return COLD_LIGHTER_MAGENTA;
            if(roundedValue <= -0.33)return COLD_LIGHT_MAGENTA;
            if(roundedValue >= -0.32 && roundedValue <= 0.33)return COLD_MAGENTA;
            if(roundedValue >= 0.34)return COLD_DARK_MAGENTA;
        }
        return null;
    }

}
