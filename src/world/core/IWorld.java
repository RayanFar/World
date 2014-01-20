package world.core;

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
    
    float getWorldHeight();
    
    int getThreadPoolCount();
    void setThreadPoolCount(int threadcount);
    
    int getPatchSize();
    int getBlockSize();
    
    boolean terrainLoaded(TerrainQuad terrainQuad);
    boolean terrainUnloaded(TerrainQuad terrainQuad);
    
    TerrainQuad getTerrainQuad(Vector3f location);
    
    boolean isLoaded();
    
    int getWorldScale();    
    
    TerrainQuad getLoadedTerrainQuad(Vector3f location);
    TerrainQuad getCachedTerrainQuad(Vector3f location);
}
