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

import com.azortis.orbis.biomedemo.objects.Biome;
import com.azortis.orbis.biomedemo.objects.Dimension;
import com.azortis.orbis.biomedemo.objects.Region;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Registry {

    private static final Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private static final Map<String, Dimension> dimensionMap = new HashMap<>();
    private static final Map<String, Region> regionMap = new HashMap<>();
    private static final Map<String, Biome> biomeMap = new HashMap<>();

    private static final Map<Integer, Biome> biomeIdMap = new HashMap<>();

    private Registry(){
        File rootDirectory = new File(System.getProperty("user.dir"));
        File dimensionDirectory = new File(rootDirectory, "/dimensions/");
        for (File dimensionFile : Objects.requireNonNull(dimensionDirectory.listFiles())) {
            try{
                Dimension dimension = gson.fromJson(new FileReader(dimensionFile), Dimension.class);
                dimensionMap.put(dimension.getName(), dimension);
            }catch (FileNotFoundException ex){
                ex.printStackTrace();
            }
        }
        File regionsDirectory = new File(rootDirectory, "/regions/");
        for (File regionFile : Objects.requireNonNull(regionsDirectory.listFiles())) {
            try{
                Region region = gson.fromJson(new FileReader(regionFile), Region.class);
                regionMap.put(region.getName(), region);
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            }
        }
        File biomesDirectory = new File(rootDirectory, "/biomes/");
        for (File biomeFile : Objects.requireNonNull(biomesDirectory.listFiles())) {
            try{
                Biome biome = gson.fromJson(new FileReader(biomeFile), Biome.class);
                biomeMap.put(biome.getName(), biome);
                biomeIdMap.put(biome.getId(), biome);
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            }
        }
    }

    @NotNull
    public static Dimension getDimension(String name){
        return dimensionMap.get(name);
    }

    @NotNull
    public static Region getRegion(String name){
        return regionMap.get(name);
    }

    @NotNull
    public static Biome getBiome(String name){
        return biomeMap.get(name);
    }

    @NotNull
    public static Biome getBiome(int id){
        return biomeIdMap.get(id);
    }

}
