package com.azortis.orbis.biomedemo.noise;

public class Cellular {

    private final FastNoise noise;

    public Cellular(long seed){
        noise = new FastNoise(seed);
        noise.setNoiseType(FastNoise.NoiseType.Cellular);
        noise.setCellularReturnType(FastNoise.CellularReturnType.CellValue);
        noise.setCellularDistanceFunction(FastNoise.CellularDistanceFunction.Hybrid);
        noise.setFrequency(1);
    }

    public double noise(double x, double z){
        return noise.getNoise(x, z);
    }

}
