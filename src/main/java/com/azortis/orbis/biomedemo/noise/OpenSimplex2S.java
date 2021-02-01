package com.azortis.orbis.biomedemo.noise;

public class OpenSimplex2S {

    private final FastNoise noise;

    public OpenSimplex2S(long seed){
        noise = new FastNoise(seed);
        noise.setNoiseType(FastNoise.NoiseType.OpenSimplex2S);
        noise.setFrequency(1);
    }

    public double noise(double x, double z){
        return noise.getNoise(x, z);
    }

}
