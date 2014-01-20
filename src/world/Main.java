package world;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import world.core.World;
import world.examples.Example_ImageHeightMap;
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
        world = new Example_NoiseHeightMap(this, 65, 129, 256);
        // world = new Example_ImageHeightMap(this, 65, 129, 256);
        
        // set our various options...
        world.setWorldScale(3);
        world.setViewDistance(2);
        
        // Finally, attach to the state manager so we can monitor movement.
        stateManager.attach(world);
    }

    @Override
    public void destroy()
    {
        super.destroy();

        // Clean up after ourselves.
        if (world != null) world.close();
    }
}
