package world;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import world.core.World;
import world.examples.Example_NoiseHeightMap;

public class Main extends SimpleApplication 
{
    private BulletAppState bulletAppState;
    private World world;
    
    public static void main(String[] args)
    {
        Main app = new Main();
        app.start();
    }

    @Override
    public void simpleInitApp() 
    {
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        
        // 1mph = 0.44704f
        float camSpeed = 0.44704f * 1300; // 300 mph
        this.flyCam.setMoveSpeed(camSpeed);
        
        // add the sun
        DirectionalLight sun = new DirectionalLight();
        sun.setColor(ColorRGBA.White);
        sun.setDirection(new Vector3f(-1f, 0, 0));
        rootNode.addLight(sun);
        
        // Add some ambient light
        AmbientLight ambientLight = new AmbientLight();
        ambientLight.setColor(ColorRGBA.White);
        rootNode.addLight(ambientLight);
        
        // set the sky color
        this.viewPort.setBackgroundColor(new ColorRGBA(0.357f, 0.565f, 0.878f, 1f));
        
        // create the world that we want...
        int patchSize = 65;
        int blockSize = 129;
        int worldHeight = 256;
        int worldScale = 1;
        
        world = new Example_NoiseHeightMap(this, patchSize, blockSize, worldHeight, worldScale);
        // world = new Example_ImageHeightMap(this, patchSize, blockSize, worldHeight, worldScale);
        
        // set our various options...
        
        // set the distance in all directions, or individually..
        world.setViewDistance(2); 
        // world.setViewDistance(2, 3, 4, 5);
        
        // the the amount of processors used for terrain generation and Level of Detail.
        // Default uses Runtime.getRuntime().availableProcessors()
        // world.setThreadPoolCount(2);
        
        // Finally, attach to the state manager so we can monitor movement or the camera,
        // which we use to determine when terrain should be added and removed from the scene.
        stateManager.attach(world);
    }
    
    private boolean hasJoined = false;
    
    @Override
    public void simpleUpdate(float tpf)
    {
        // wait until the world has loaded until we let the player join.
        // In this case, we'll just move the camera to a better position.
        if (world == null || world.isLoaded() == false || hasJoined) return;
        
        this.getCamera().setLocation(new Vector3f(0, 250, 0));
        this.hasJoined = true;
    }

    @Override
    public void destroy()
    {
        super.destroy();

        // Clean up after ourselves.
        if (world != null) world.close();
    }
}
