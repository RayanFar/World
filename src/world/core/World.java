package world.core;

import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.bullet.BulletAppState;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.terrain.geomipmap.TerrainLodControl;
import com.jme3.terrain.geomipmap.TerrainQuad;
import java.io.Closeable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public abstract class World extends AbstractAppState implements IWorld, Closeable
{
    private final SimpleApplication app;
    
    private int 
            vd_north = 2, vd_east = 2, vd_south = 2, vd_west = 2,
            oldLocX = Integer.MAX_VALUE, oldLocZ = Integer.MAX_VALUE,
            positionAdjustment = 0,
            topLx, topLz, botRx, botRz,
            totalVisibleChunks = 25;
    
    private boolean isLoaded = false;
    private volatile boolean cacheInterrupted = false;
    
    private final int patchSize, blockSize;
    
    private float worldHeight = 256f;
    
    private int worldScale = 1;
    
    private TerrainListener terrainListener;
    private ScheduledThreadPoolExecutor threadpool = new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors());
    
    protected final Map<Vector3f, TerrainQuad> worldTiles = new HashMap<Vector3f, TerrainQuad>();
    private final Set<Vector3f> worldTilesQue = new HashSet<Vector3f>();
    protected final Map<Vector3f, TerrainQuad> worldTilesCache = new ConcurrentHashMap<Vector3f, TerrainQuad>();
    private final ConcurrentLinkedQueue<TerrainQuad> newTiles = new ConcurrentLinkedQueue<TerrainQuad>();
    
    public World(SimpleApplication app, int patchSize, int blockSize)
    {
        this.app = app;
        this.patchSize = patchSize;
        this.blockSize = blockSize;
    }
    
    public SimpleApplication getApplication() { return this.app; }
    
    public void setViewDistance(int north, int east, int south, int west)
    {
        this.vd_north = north;
        this.vd_east = east;
        this.vd_south = south;
        this.vd_west = west;
        
        totalVisibleChunks = (vd_west + vd_east + 1) * (vd_north + vd_south + 1);
    }

    public void setViewDistance(int distance)
    {
        setViewDistance(distance, distance, distance, distance);
    }

    public int getViewDistanceNorth() { return vd_north; }
    public int getViewDistanceEast() { return vd_east; }
    public int getViewDistanceSouth() { return vd_south; }
    public int getViewDistanceWest() { return vd_west; }

    public void setWorldHeight(float height) { this.worldHeight = height; }
    public float getWorldHeight() { return this.worldHeight; }

    public int getThreadPoolCount() { return threadpool.getPoolSize(); }
    public void setThreadPoolCount(int threadcount) { threadpool.setCorePoolSize(threadcount); }

    protected int getPositionAdjustment() { return this.positionAdjustment; }
    protected void setPositionAdjustment(int value) { this.positionAdjustment = value; }

    public int getPatchSize() { return this.patchSize; }
    public int getBlockSize() { return this.blockSize; }

    public boolean terrainLoaded(TerrainQuad terrainQuad) { return this.terrainListener.terrainLoaded(terrainQuad);}
    public void terrainLoadedThreaded(TerrainQuad terrainQuad) { this.terrainListener.terrainLoadedThreaded(terrainQuad); }
    public boolean terrainUnloaded(TerrainQuad terrainQuad) { return this.terrainListener.terrainUnloaded(terrainQuad); }

    public TerrainListener getTerrainListener() { return this.terrainListener; }
    public void setTerrainListener(TerrainListener listener) { this.terrainListener = listener; }
    
    public boolean isLoaded() { return this.isLoaded; }
    
    public int getWorldScale() { return this.worldScale; }
    public void setWorldScale(int scale) 
    { 
        this.worldScale = scale;
        this.setPositionAdjustment(((blockSize - 1) / 2) / this.worldScale);
    }
    
    public int bitCalc(int blockSize)
    {
        switch (blockSize)
        {
            case 17: return 4;
            case 33: return 5;
            case 65: return 6;
            case 129: return 7;
            case 257: return 8;
            case 513: return 9;
            case 1025: return 10;
        }

        throw new IllegalArgumentException("Invalid block size specified.");
    }
    
    public Vector3f toTerrainLocation(Vector3f location)
    {
        int x = (int)location.getX() >> this.bitCalc(blockSize);
        int z = (int)location.getZ() >> this.bitCalc(blockSize);
        
        return new Vector3f(x, 0, z);
    }
    
    public Vector3f fromTerrainLocation(Vector3f location)
    {
        int x = (int)location.getX() << this.bitCalc(blockSize);
        int z = (int)location.getZ() << this.bitCalc(blockSize);
        
        return new Vector3f(x, 0, z);
    }
    
    public abstract TerrainQuad getTerrainQuad(Vector3f location);
    
    private boolean checkForOldChunks()
    {
        Iterator<Map.Entry<Vector3f, TerrainQuad>> iterator = worldTiles.entrySet().iterator();

        while(iterator.hasNext())
        {
            Map.Entry<Vector3f, TerrainQuad> entry = iterator.next();
            Vector3f quadLocation = entry.getKey();
            

            if (quadLocation.getX() < topLx || quadLocation.getX() > botRx || quadLocation.getZ() < topLz || quadLocation.getZ() > botRz)
            {
                TerrainQuad chunk = entry.getValue();

                // throw the tile unloaded event and check if the tile unload has been cancelled
                if (!this.terrainUnloaded(chunk))
                    return false;

                app.getStateManager().getState(BulletAppState.class).getPhysicsSpace().remove(chunk);
                app.getRootNode().detachChild(chunk);

                iterator.remove();

                return true;
            }
        }

        return false;
    }
    
    private boolean checkForNewChunks()
    {
        // tiles are always removed first to keep triangle count down, so we can
        // safely assume this is a reasonable comparative.
        if (worldTiles.size() == totalVisibleChunks)
        {
            isLoaded = true; // used to determine whether the player can join the world.
            return false;
        }

        // check if any requested tiles are ready to be added.
        TerrainQuad pending = newTiles.poll();
        
        if (pending != null)
        {
            // throw the TileLoaded event & check if the tile load has been cancelled.
            if (!terrainLoaded(pending))
                return false;

            TerrainLodControl lodControl = new TerrainLodControl(pending, app.getCamera());
            lodControl.setExecutor(threadpool);
            pending.addControl(lodControl);
            
            Vector3f scaledPos = new Vector3f(pending.getWorldTranslation().getX() / this.getWorldScale(), 0, pending.getWorldTranslation().getZ() / this.getWorldScale());
            
            worldTiles.put(this.toTerrainLocation(scaledPos), pending);
            app.getRootNode().attachChild(pending);
            
            app.getStateManager().getState(BulletAppState.class).getPhysicsSpace().add(pending);

            return true;
        }
        else
        {
            for (int x = topLx; x <= botRx; x++)
            {
                for (int z = topLz; z <= botRz; z++)
                {
                    final Vector3f location = new Vector3f(x, 0, z);

                    // check its already loaded.
                    if (worldTiles.get(location) != null)
                        continue;

                    // check if it's already in the que.
                    if (worldTilesQue.contains(location))
                        continue;

                    // check if its in the cache.
                    TerrainQuad chunk = worldTilesCache.get(location);
                    
                    if (chunk != null)
                    {
                        // throw the TileLoaded event & check if the tile load has been cancelled.
                        if (!terrainLoaded(chunk))
                            return false;

                        TerrainLodControl lodControl = new TerrainLodControl(chunk, app.getCamera());
                        lodControl.setExecutor(threadpool);
                        chunk.addControl(lodControl);

                        chunk.setShadowMode(ShadowMode.Receive);

                        app.getRootNode().attachChild(chunk);
                        app.getStateManager().getState(BulletAppState.class).getPhysicsSpace().add(chunk);
                        worldTiles.put(location, chunk);

                        return true;
                    }
                    else
                    {
                        // its nowhere to be seen, generate it.
                        worldTilesQue.add(location);

                        threadpool.submit(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                TerrainQuad newChunk = getTerrainQuad(location);
                                
                                // fire the terrainLoadedThreaded event
                                terrainLoadedThreaded(newChunk);

                                newTiles.add(newChunk);

                                // thread safety...
                                app.enqueue(new Callable<Boolean>()
                                {
                                    public Boolean call()
                                    {
                                        worldTilesQue.remove(location);
                                        return true;
                                    }
                                });
                            }
                        });

                        return true;
                    }
                }
            }
        }

        return false;
    }
    
    private void recalculateCache()
    {
        worldTilesCache.clear();
        cacheInterrupted = false;

        Runnable cacheUpdater = new Runnable()
        {
            @Override
            public void run()
            {
                // top and bottom
                for (int x = (topLx -1); x <= (botRx + 1); x++)
                {
                    if (cacheInterrupted) return;

                    // top
                    final Vector3f topLocation = new Vector3f(x, 0, topLz - 1);
                    final TerrainQuad topChunk = getTerrainQuad(topLocation);
                    terrainLoadedThreaded(topChunk);

                    // bottom
                    final Vector3f bottomLocation = new Vector3f(x, 0, botRz + 1);
                    final TerrainQuad bottomChunk = getTerrainQuad(bottomLocation);
                    terrainLoadedThreaded(bottomChunk);

                    app.enqueue(new Callable<Boolean>()
                    {
                        public Boolean call()
                        {
                            worldTilesCache.put(topLocation, topChunk);
                            worldTilesCache.put(bottomLocation, bottomChunk);

                            return true;
                        }
                    });
                }

                // sides
                for (int z = topLz; z <= botRz; z++)
                {
                    if (cacheInterrupted) return;

                    // left
                    final Vector3f leftLocation = new Vector3f(topLx - 1, 0, z);
                    final TerrainQuad leftChunk = getTerrainQuad(leftLocation);
                    leftChunk.setShadowMode(ShadowMode.Receive);
                    terrainLoadedThreaded(leftChunk);

                    // right
                    final Vector3f rightLocation = new Vector3f(botRx + 1, 0, z);
                    final TerrainQuad rightChunk = getTerrainQuad(rightLocation);
                    rightChunk.setShadowMode(ShadowMode.Receive);
                    terrainLoadedThreaded(rightChunk);

                    app.enqueue(new Callable<Boolean>()
                    {
                        public Boolean call()
                        {
                            worldTilesCache.put(leftLocation, leftChunk);
                            worldTilesCache.put(rightLocation, rightChunk);

                            return true;
                        }
                    });
                }
            }
        };

        threadpool.execute(cacheUpdater);
    }
    
    @Override public void update(float tpf)
    {
        int actualX = (int)(app.getCamera().getLocation().getX() + positionAdjustment);
        int actualZ = (int)(app.getCamera().getLocation().getZ() + positionAdjustment);
        
        int locX = (int)actualX >> this.bitCalc(blockSize);
        int locZ = (int)actualZ >> this.bitCalc(blockSize);
        
        if ((locX == oldLocX) && (locZ == oldLocZ) && worldTilesQue.isEmpty() && newTiles.isEmpty())
        {
            return;
        }
        
        topLx = locX - vd_west;
        topLz = locZ - vd_north;
        botRx = locX + vd_east;
        botRz = locZ + vd_south;
        
        if (checkForOldChunks())
            return;
        
        if (checkForNewChunks())
            return;
        
        if (worldTilesQue.isEmpty() && newTiles.isEmpty())
        {
            cacheInterrupted = true;
            recalculateCache();

            oldLocX = locX;
            oldLocZ = locZ;
        }
    }
    
    
    @Override public void close() 
    { 
        threadpool.shutdown(); 
    }
}
