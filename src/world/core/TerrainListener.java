package world.core;

import com.jme3.terrain.geomipmap.TerrainQuad;

public interface TerrainListener
{
    boolean terrainLoaded(TerrainQuad terrainQuad);
    void terrainLoadedThreaded(TerrainQuad terrainQuad);
    
    boolean terrainUnloaded(TerrainQuad terrainQuad);
    
    String heightMapImageRequired(int x, int z);
}
