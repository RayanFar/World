package world.core;

import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.terrain.geomipmap.TerrainQuad;

public interface IWorld 
{
    void setViewDistance(int north, int east, int south, int west);
    void setViewDistance(int distance);
    
    int getViewDistanceNorth();
    int getViewDistanceEast();
    int getViewDistanceSouth();
    int getViewDistanceWest();
    
    void setWorldHeight(float height);
    float getWorldHeight();
    
    int getThreadPoolCount();
    void setThreadPoolCount(int threadcount);
    
    Material getMaterial();
    void setMaterial(Material material);
    
    int getPatchSize();
    int getBlockSize();
    
    boolean terrainLoaded(TerrainQuad terrainQuad);
    void terrainLoadedThreaded(TerrainQuad terrainQuad);
    boolean terrainUnloaded(TerrainQuad terrainQuad);
    
    TerrainListener getTerrainListener();
    void setTerrainListener(TerrainListener listener);
    
    TerrainQuad getTerrainQuad(Vector3f location);
    
    int getPositionAdjustment();
    void setPositionAdjustment(int value);
    
    boolean isLoaded();
    
    Vector3f toTerrainLocation(Vector3f location);
    Vector3f fromTerrainLocation(Vector3f location);
    
    void setWorldScale(Vector3f scale);
    Vector3f getWorldScale();
}
