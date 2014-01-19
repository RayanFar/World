
package world;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.texture.Texture;
import world.core.ImageBasedWorld;
import world.core.TerrainListener;

public class TestImage extends SimpleApplication 
{
    private BulletAppState bulletAppState;
    private ImageBasedWorld world;
    
    public static void main(String[] args)
    {
        TestImage app = new TestImage();
        app.start();
    }

    @Override
    public void simpleInitApp()
    {
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        
        // 1mph = 0.44704f
        float camSpeed = 0.44704f * 300; // 300 mph
        this.flyCam.setMoveSpeed(camSpeed);
        
        // add the sun
        DirectionalLight sun = new DirectionalLight();
        sun.setColor(ColorRGBA.White);
        sun.setDirection(new Vector3f(-1f, 0, 0));
        rootNode.addLight(sun);
        
        // set the sky color
        this.viewPort.setBackgroundColor(new ColorRGBA(0.357f, 0.565f, 0.878f, 1f));
        
        // For the sake of simplicity in this example, we create a material that will be used universally in our world.
        final Material terrainMaterial = new Material(this.assetManager, "Common/MatDefs/Terrain/HeightBasedTerrain.j3md");
        
        // GRASS texture
        Texture grass = this.assetManager.loadTexture("Textures/Terrain/splat/grass.jpg");
        grass.setWrap(Texture.WrapMode.Repeat);
        terrainMaterial.setTexture("region1ColorMap", grass);
        terrainMaterial.setVector3("region1", new Vector3f(88, 200, 16));
        
        // DIRT texture
        Texture dirt = this.assetManager.loadTexture("Textures/Terrain/splat/dirt.jpg");
        dirt.setWrap(Texture.WrapMode.Repeat);
        terrainMaterial.setTexture("region2ColorMap", dirt);
        terrainMaterial.setVector3("region2", new Vector3f(0, 90, 16));
        
        // ROCK texture
        Texture rock = this.assetManager.loadTexture("Textures/Terrain/Rock/Rock.PNG");
        rock.setWrap(Texture.WrapMode.Repeat);
        terrainMaterial.setTexture("region3ColorMap", rock);
        terrainMaterial.setVector3("region3", new Vector3f(198, 260, 16));
        
        terrainMaterial.setTexture("region4ColorMap", rock);
        terrainMaterial.setVector3("region4", new Vector3f(198, 260, 16));
        
        Texture rock2 = this.assetManager.loadTexture("Textures/Terrain/Rock2/rock.jpg");
        rock2.setWrap(Texture.WrapMode.Repeat);
        terrainMaterial.setTexture("slopeColorMap", rock2);
        terrainMaterial.setFloat("slopeTileFactor", 32);
        
        terrainMaterial.setFloat("terrainSize", 129);
        
        // Create a terrainListener...
        TerrainListener terrainListener = new TerrainListener() {

            public boolean terrainLoaded(TerrainQuad terrainQuad)
            {
                // Set the material for every tile loaded.
                // We could set a different material based on position or something
                // if we wished. For now, we'll just use a universal material.
                terrainQuad.setMaterial(terrainMaterial);
                terrainQuad.setShadowMode(RenderQueue.ShadowMode.Receive);
                
                // We can return false here to cancel the load event of terrain
                // for any such purpose. For now, we'll just allow all load events.
                return true;
            }

            public boolean terrainUnloaded(TerrainQuad terrainQuad)
            {
                // We can return false here to cancel the unload event of terrain
                // for any such purpose. For now, we'll just allow all unload events.
                return true;
            }

            public String heightMapImageRequired(int x, int z)
            {
                String path = new StringBuilder()
                        .append("Textures/heightmaps/hmap_")
                        .append(x)
                        .append("_")
                        .append(z)
                        .append(".jpg")
                        .toString();
                
                return path; 
            }

            public void terrainLoadedThreaded(TerrainQuad terrainQuad)
            {
                // This event is fired when terrain has loaded, and allows us to
                // do things like load vegetation or what not on a seperate level
                // before enqueing it to load on the GL thread.
                // For now, we'll just ignroe it.
            }
        };
        
        // Create the world based on heightmap images.
        world = new ImageBasedWorld(this, terrainListener, 65, 129, 256);
        // world.setWorldScale(new Vector3f(2, 0, 2));
        
        // Attach to the state manager so we can monitor movement.
        this.stateManager.attach(world);
    }
    
    @Override
    public void simpleUpdate(float tpf) 
    {
        
    }

    @Override
    public void simpleRender(RenderManager rm) 
    {
        
    }
    
    @Override
    public void destroy()
    {
        super.destroy();

        if (world != null)
            world.close();
    }
}
