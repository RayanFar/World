package world.examples;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetNotFoundException;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Node;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.heightmap.AbstractHeightMap;
import com.jme3.terrain.heightmap.ImageBasedHeightMap;
import com.jme3.texture.Texture;
import java.util.logging.Level;
import java.util.logging.Logger;
import world.core.World;

public class Example_ImageHeightMap extends World
{
    private final Material terrainMaterial;
    
    public Example_ImageHeightMap(SimpleApplication app, int patchSize, int blockSize, float worldHeight, int worldScale)
    {
        super(app, patchSize, blockSize, worldHeight, worldScale);
        
        // Create a universal material to apply on all terrain.
        this.terrainMaterial = createTerrainMaterial();
    }

    @Override
    public boolean worldItemLoaded(Node node)
    {
        // apply the material we created earlier to the terrain...
        node.setMaterial(terrainMaterial);
        
        // set the shadow mode, if so desired...
        node.setShadowMode(ShadowMode.Receive);
        
        // return true if we want to allow this terrain to load.
        // return false if we want to cancel this terrain loading.
        return true;
    }

    @Override
    public boolean worldItemUnloaded(Node node)
    {
        // return true if we want to allow this terrain to unload.
        // return false if we want to cancel this terrain from unloading.
        return true;
    }

    @Override
    public Node getWorldItem(Vector3f location)
    {
        // check if the requested terrain is already loaded.
        Node tq = this.getLoadedItem(location);
        if (tq != null) return tq;
        
        // check if the requested terrain is already cached.
        tq = (TerrainQuad)this.getCachedItem(location);
        if (tq != null) return tq;
        
        // doesnt exist anywhere, so we'll create it.
        String tqName = "TerrainQuad_" + (int)location.getX() + "_" + (int)location.getZ();
        
        String imagePath = new StringBuilder()
                .append("Textures/heightmaps/hmap_")
                .append((int)location.getX())
                .append("_")
                .append((int)location.getZ())
                .append(".jpg")
                .toString();
        
        try
        {
            Texture hmapImage = this.getApplication().getAssetManager().loadTexture(imagePath);
            AbstractHeightMap map = new ImageBasedHeightMap(hmapImage.getImage());
            map.load();

            tq = new TerrainQuad(tqName, this.getPatchSize(), this.getBlockSize(), map.getHeightMap());
        }
        catch (AssetNotFoundException ex)
        {
            Logger.getLogger("com.jme").log(Level.INFO, "Image not found: {0}", imagePath);
        }
        
        return tq;
    }
    
    private Material createTerrainMaterial()
    {
        Material material = new Material(this.getApplication().getAssetManager(), "Common/MatDefs/Terrain/HeightBasedTerrain.j3md");
        
        // GRASS texture
        Texture grass = this.getApplication().getAssetManager().loadTexture("Textures/Terrain/splat/grass.jpg");
        grass.setWrap(Texture.WrapMode.Repeat);
        material.setTexture("region1ColorMap", grass);
        material.setVector3("region1", new Vector3f(88, 200, 16));
        
        // DIRT texture
        Texture dirt = this.getApplication().getAssetManager().loadTexture("Textures/Terrain/splat/dirt.jpg");
        dirt.setWrap(Texture.WrapMode.Repeat);
        material.setTexture("region2ColorMap", dirt);
        material.setVector3("region2", new Vector3f(0, 90, 16));
        
        // ROCK texture
        Texture rock = this.getApplication().getAssetManager().loadTexture("Textures/Terrain/Rock/Rock.PNG");
        rock.setWrap(Texture.WrapMode.Repeat);
        material.setTexture("region3ColorMap", rock);
        material.setVector3("region3", new Vector3f(198, 260, 16));
        
        material.setTexture("region4ColorMap", rock);
        material.setVector3("region4", new Vector3f(198, 260, 16));
        
        Texture rock2 = this.getApplication().getAssetManager().loadTexture("Textures/Terrain/Rock2/rock.jpg");
        rock2.setWrap(Texture.WrapMode.Repeat);
        material.setTexture("slopeColorMap", rock2);
        material.setFloat("slopeTileFactor", 32);
        
        material.setFloat("terrainSize", this.getBlockSize());
        
        
        return material;
    }
    
    
}
