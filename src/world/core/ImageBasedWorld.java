package world.core;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetNotFoundException;
import com.jme3.bullet.collision.shapes.HeightfieldCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.math.Vector3f;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.heightmap.AbstractHeightMap;
import com.jme3.terrain.heightmap.ImageBasedHeightMap;
import com.jme3.texture.Texture;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ImageBasedWorld extends World
{
    public ImageBasedWorld(SimpleApplication app, TerrainListener terrainListener, int patchSize, int blockSize, float height)
    {
        super(app, patchSize, blockSize);
        
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
        
        String tqName = "TerrainQuad_" + (int)location.getX() + "_" + (int)location.getZ();
        
        // fire the imageHeightmapRequired event to obtain the image path
        String imagePath = this.getTerrainListener().heightMapImageRequired((int)location.getX(), (int)location.getZ());
        float[] heightmap;
        
        try
        {
            Texture hmapImage = this.getApplication().getAssetManager().loadTexture(imagePath);
            AbstractHeightMap map = new ImageBasedHeightMap(hmapImage.getImage());
            map.load();

            heightmap = map.getHeightMap();
        }
        catch (AssetNotFoundException ex)
        {
            // The assetManager already logs null assets. don't re-iterate the point?
            Logger.getLogger("com.jme").log(Level.INFO, "Image not found: {0}", imagePath);
            
            heightmap = new float[this.getBlockSize() * this.getBlockSize()];
            Arrays.fill(heightmap, 0f);
        }
        
        tq = new TerrainQuad(tqName, this.getPatchSize(), this.getBlockSize(), heightmap);
        
        Vector3f pos = this.fromTerrainLocation(location);
        float scaledX = pos.getX() * this.getWorldScale().getX();
        float scaledZ = pos.getZ() * this.getWorldScale().getZ();
        
        tq.setLocalTranslation(scaledX, 0, scaledZ);
        
        tq.addControl(new RigidBodyControl(new HeightfieldCollisionShape(heightmap, tq.getLocalScale()), 0));
        
        return tq;
    }
    
}
