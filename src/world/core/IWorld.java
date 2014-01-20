package world.core;

import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

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
    
    boolean worldItemLoaded(Node node);
    boolean worldItemUnloaded(Node node);
    
    Node getWorldItem(Vector3f location);
    
    boolean isLoaded();
    
    int getWorldScale();    
    
    Node getLoadedItem(Vector3f location);
    Node getCachedItem(Vector3f location);
}
