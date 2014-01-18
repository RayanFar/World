package world.core;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.collision.shapes.HeightfieldCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.math.Vector3f;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.noise.basis.FilteredBasis;
import java.nio.FloatBuffer;

public class NoiseBasedWorld extends World
{
    private final FilteredBasis filteredBasis;
    
    public NoiseBasedWorld(SimpleApplication app, FilteredBasis filteredBasis, TerrainListener terrainListener, int patchSize, int blockSize, float height)
    {
        super(app, patchSize, blockSize);
        
        this.filteredBasis = filteredBasis;
        
        this.setTerrainListener(terrainListener);
        this.setWorldHeight(height);
        this.setPositionAdjustment((blockSize - 1) / 2);
    }

    public FilteredBasis getFilteredBasis() { return this.filteredBasis; }

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
        
        float[] heightmap = getHeightmap(location);
        tq = new TerrainQuad(tqName, this.getPatchSize(), this.getBlockSize(), heightmap);
        tq.setLocalScale(new Vector3f(this.getWorldScale().getX(), this.getWorldHeight(), this.getWorldScale().getZ()));
        
        Vector3f pos = this.fromTerrainLocation(location);
        float scaledX = pos.getX() * this.getWorldScale().getX();
        float scaledZ = pos.getZ() * this.getWorldScale().getZ();
        
        tq.setLocalTranslation(scaledX, 0, scaledZ);
        
        tq.addControl(new RigidBodyControl(new HeightfieldCollisionShape(heightmap, tq.getLocalScale()), 0));
        
        return tq;
    }
    
    private float[] getHeightmap(Vector3f tl) 
    { 
        return getHeightmap((int)tl.getX(), (int)tl.getZ()); 
    }
    
    private float[] getHeightmap(int x, int z)
    {
        FloatBuffer buffer = this.filteredBasis.getBuffer(x * (this.getBlockSize() - 1), z * (this.getBlockSize() - 1), 0, this.getBlockSize());
        return buffer.array();
    }
    
}
