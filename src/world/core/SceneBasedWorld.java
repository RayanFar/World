package world.core;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.collision.shapes.HeightfieldCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.terrain.geomipmap.TerrainQuad;
import java.util.Arrays;

public class SceneBasedWorld extends World
{
    private Spatial originalScene;
    
    public SceneBasedWorld(SimpleApplication app, Spatial scene, TerrainListener terrainListener, int patchSize, int blockSize, float height)
    {
        super(app, patchSize, blockSize);
        
        this.originalScene = scene;
        
        this.setTerrainListener(terrainListener);
        this.setWorldHeight(height);
        this.setPositionAdjustment((blockSize - 1) / 2);
    }

    @Override
    public TerrainQuad getTerrainQuad(Vector3f location)
    {
        TerrainQuad tq = this.worldTiles.get(location);
        
        if (tq != null)
            return tq;
        
        tq = this.worldTilesCache.get(location);
        
        if (tq != null)
            return tq;
        
        Vector3f position = this.fromTerrainLocation(location);
        tq = findTerrainQuad(position, (Node)originalScene);
        
        
        
        if (tq == null)
        {
            String tqName = "TerrainQuad_" + (int)location.getX() + "_" + (int)location.getZ();
            
            float[] heightmap = new float[this.getBlockSize() * this.getBlockSize()];
            Arrays.fill(heightmap, 0f);
            
            tq = new TerrainQuad(tqName, this.getPatchSize(), this.getBlockSize(), heightmap);
            
        }
        
        Vector3f pos = this.fromTerrainLocation(location);
        float scaledX = pos.getX() * this.getWorldScale();
        float scaledZ = pos.getZ() * this.getWorldScale();
        
        tq.setLocalTranslation(scaledX, 0, scaledZ);
        
        tq.addControl(new RigidBodyControl(new HeightfieldCollisionShape(tq.getHeightMap(), tq.getLocalScale()), 0));
        
        return tq;
        
    }
    
    private TerrainQuad findTerrainQuad(Vector3f position, Node parent)
    {
        for (Spatial child : parent.getChildren())
        {
            if (child instanceof TerrainQuad)
            {
                Vector3f tqLoc = child.getWorldTranslation();
                
                if (tqLoc.equals(position))
                {
                    TerrainQuad tq = (TerrainQuad)child;
                    
                    return tq;
                }
                    // return (TerrainQuad)child;
            }
            
            else if (child instanceof Node)
            {
                Node newChild = (Node)child;
                
                if (newChild.getChildren().isEmpty())
                    continue;
                
                findTerrainQuad(position, newChild);
            }
        }
        
        return null;
    }
    
}
