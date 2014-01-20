package world.examples;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.collision.shapes.HeightfieldCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Node;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.noise.ShaderUtils;
import com.jme3.terrain.noise.basis.FilteredBasis;
import com.jme3.terrain.noise.filter.IterativeFilter;
import com.jme3.terrain.noise.filter.OptimizedErode;
import com.jme3.terrain.noise.filter.PerturbFilter;
import com.jme3.terrain.noise.filter.SmoothFilter;
import com.jme3.terrain.noise.fractal.FractalSum;
import com.jme3.terrain.noise.modulator.NoiseModulator;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import java.nio.FloatBuffer;
import world.core.World;

public class Example_NoiseHeightMap extends World
{
    private final FilteredBasis filteredBasis;
    private final Material terrainMaterial;
    
    public Example_NoiseHeightMap(SimpleApplication app, int patchSize, int blockSize, float worldHeight, int worldScale)
    {
        super(app, patchSize, blockSize, worldHeight, worldScale);
        
        // Create a noise generator. In this example we will use the internal jme implementation,
        // But in reality any noise generation method could be used.
        this.filteredBasis = createNoiseGenerator();
        
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
        tq = this.getCachedItem(location);
        if (tq != null) return tq;
        
        // doesnt exist anywhere, so we'll create it.
        String tqName = "TerrainQuad_" + (int)location.getX() + "_" + (int)location.getZ();
        float[] heightmap = getHeightmap((int)location.getX(), (int)location.getZ());
        tq = new TerrainQuad(tqName, this.getPatchSize(), this.getBlockSize(), heightmap);
        
        // set the scale as defined in our world settings...
        tq.setLocalScale(new Vector3f(this.getWorldScale(), this.getWorldHeight(), this.getWorldScale()));
        
        // set the position of the new terrain, taking world scale int account.
        Vector3f pos = this.fromTerrainLocation(location);
        float scaledX = pos.getX() * this.getWorldScale();
        float scaledZ = pos.getZ() * this.getWorldScale();
        tq.setLocalTranslation(scaledX, 0, scaledZ);
        
        // add some rigidity to the terrain so objects don't fall through.
        tq.addControl(new RigidBodyControl(new HeightfieldCollisionShape(heightmap, tq.getLocalScale()), 0));
        
        // finally, return the requested terrain.
        return tq;
    }
    
    private float[] getHeightmap(int x, int z)
    {
        FloatBuffer buffer = this.filteredBasis.getBuffer(x * (this.getBlockSize() - 1), z * (this.getBlockSize() - 1), 0, this.getBlockSize());
        return buffer.array();
    }
    
    private FilteredBasis createNoiseGenerator()
    {
        FractalSum base = new FractalSum();
        base.setRoughness(0.7f);
        base.setFrequency(1.0f);
        base.setAmplitude(1.0f);
        base.setLacunarity(3.12f);
        base.setOctaves(8);
        base.setScale(0.02125f);
        base.addModulator(new NoiseModulator()
            {
                @Override public float value(float... in) 
                {
                    return ShaderUtils.clamp(in[0] * 0.5f + 0.5f, 0, 1);
                }
            });
        
        FilteredBasis ground = new FilteredBasis(base);
        PerturbFilter perturb = new PerturbFilter();
        perturb.setMagnitude(0.119f);
        
        OptimizedErode therm = new OptimizedErode();
        therm.setRadius(5);
        therm.setTalus(0.011f);
        
        SmoothFilter smooth = new SmoothFilter();
        smooth.setRadius(1);
        smooth.setEffect(0.7f);
        
        IterativeFilter iterate = new IterativeFilter();
        iterate.addPreFilter(perturb);
        iterate.addPostFilter(smooth);
        iterate.setFilter(therm);
        iterate.setIterations(1);
        
        ground.addPreFilter(iterate);
        
        return ground;
    }
    
    private Material createTerrainMaterial()
    {
        Material material = new Material(this.getApplication().getAssetManager(), "Common/MatDefs/Terrain/HeightBasedTerrain.j3md");
        
        // GRASS texture
        Texture grass = this.getApplication().getAssetManager().loadTexture("Textures/Terrain/splat/grass.jpg");
        grass.setWrap(WrapMode.Repeat);
        material.setTexture("region1ColorMap", grass);
        material.setVector3("region1", new Vector3f(88, 200, 16));
        
        // DIRT texture
        Texture dirt = this.getApplication().getAssetManager().loadTexture("Textures/Terrain/splat/dirt.jpg");
        dirt.setWrap(WrapMode.Repeat);
        material.setTexture("region2ColorMap", dirt);
        material.setVector3("region2", new Vector3f(0, 90, 16));
        
        // ROCK texture
        Texture rock = this.getApplication().getAssetManager().loadTexture("Textures/Terrain/Rock/Rock.PNG");
        rock.setWrap(WrapMode.Repeat);
        material.setTexture("region3ColorMap", rock);
        material.setVector3("region3", new Vector3f(198, 260, 16));
        
        material.setTexture("region4ColorMap", rock);
        material.setVector3("region4", new Vector3f(198, 260, 16));
        
        Texture rock2 = this.getApplication().getAssetManager().loadTexture("Textures/Terrain/Rock2/rock.jpg");
        rock2.setWrap(WrapMode.Repeat);
        material.setTexture("slopeColorMap", rock2);
        material.setFloat("slopeTileFactor", 32);
        
        material.setFloat("terrainSize", this.getBlockSize());
        
        
        return material;
    }
    
}
