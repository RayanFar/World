package world.core;

import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.bullet.BulletAppState;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
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
            totalVisibleChunks = 25,
            worldScale = 1;
    
    private boolean isLoaded = false;
    private volatile boolean cacheInterrupted = false;
    
    private int patchSize, blockSize;
    private float worldHeight = 256f;
    
    private final ScheduledThreadPoolExecutor threadpool = new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors());

    
    private final Set<Vector3f> itemsQue = new HashSet<Vector3f>();
    private final Map<Vector3f, Node> itemsCache = new ConcurrentHashMap<Vector3f, Node>();
    
    private final ConcurrentLinkedQueue<Node> newItems = new ConcurrentLinkedQueue<Node>();
    private final Map<Vector3f, Node> activeItems = new HashMap<Vector3f, Node>();
    
    // used for procedural terrain (noise, image, etc)
    public World(SimpleApplication app, int patchSize, int blockSize, float height, int worldScale)
    {
        this.app = app;
        this.patchSize = patchSize;
        this.blockSize = blockSize;
        this.worldScale = worldScale;
        this.positionAdjustment = (blockSize - 1) / 2;
    }
    
    // used for pre-created scenes
    public World(SimpleApplication app, Spatial scene)
    {
        this.app = app;
        
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

    public float getWorldHeight() { return this.worldHeight; }

    public int getThreadPoolCount() { return threadpool.getPoolSize(); }
    public void setThreadPoolCount(int threadcount) { threadpool.setCorePoolSize(threadcount); }

    protected final int getPositionAdjustment() { return this.positionAdjustment; }
    protected final void setPositionAdjustment(int value) { this.positionAdjustment = value; }

    public int getPatchSize() { return this.patchSize; }
    public int getBlockSize() { return this.blockSize; }

    public abstract boolean worldItemLoaded(Node node);
    public abstract boolean worldItemUnloaded(Node node);
    public abstract Node getWorldItem(Vector3f location);

    public boolean isLoaded() { return this.isLoaded; }
    
    public int getWorldScale() { return this.worldScale; }
    
    public Node getLoadedItem(Vector3f location) { return this.activeItems.get(location); }
    public Node getCachedItem(Vector3f location) { return this.activeItems.get(location); }
    
    private int bitCalc(int blockSize)
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
    
    public int getBitShiftCount() { return this.bitCalc(blockSize); }
    
    public Vector3f toTerrainLocation(Vector3f location)
    {
        int x = (int)location.getX() >> this.getBitShiftCount();
        int z = (int)location.getZ() >> this.getBitShiftCount();
        
        return new Vector3f(x, 0, z);
    }
    
    public Vector3f fromTerrainLocation(Vector3f location)
    {
        int x = (int)location.getX() << this.getBitShiftCount();
        int z = (int)location.getZ() << this.getBitShiftCount();
        
        return new Vector3f(x, 0, z);
    }
    
    
    
    private boolean checkForOldItems()
    {
        Iterator<Map.Entry<Vector3f, Node>> iterator = activeItems.entrySet().iterator();

        while(iterator.hasNext())
        {
            Map.Entry<Vector3f, Node> entry = iterator.next();
            Vector3f quadLocation = entry.getKey();

            if (quadLocation.getX() < topLx || quadLocation.getX() > botRx || quadLocation.getZ() < topLz || quadLocation.getZ() > botRz)
            {
                TerrainQuad chunk = (TerrainQuad)entry.getValue();

                // throw the tile unloaded event and check if the tile unload has been cancelled
                if (!this.worldItemUnloaded(chunk))
                    return false;

                app.getStateManager().getState(BulletAppState.class).getPhysicsSpace().remove(chunk);
                app.getRootNode().detachChild(chunk);

                iterator.remove();

                return true;
            }
        }

        return false;
    }
    
    private boolean checkForNewItems()
    {
        // tiles are always removed first to keep triangle count down, so we can
        // safely assume this is a reasonable comparative.
        if (activeItems.size() == totalVisibleChunks)
        {
            isLoaded = true; // used to determine whether the player can join the world.
            return false;
        }

        // check if any requested tiles are ready to be added.
        Node pending = newItems.poll();
        
        if (pending != null)
        {
            // throw the TileLoaded event & check if the tile load has been cancelled.
            if (!worldItemLoaded(pending))
                return false;

            TerrainLodControl lodControl = new TerrainLodControl((TerrainQuad)pending, app.getCamera());
            lodControl.setExecutor(threadpool);
            pending.addControl(lodControl);
            
            Vector3f scaledPos = new Vector3f(pending.getWorldTranslation().getX() / this.getWorldScale(), 0, pending.getWorldTranslation().getZ() / this.getWorldScale());
            
            activeItems.put(this.toTerrainLocation(scaledPos), pending);
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
                    if (activeItems.get(location) != null)
                        continue;

                    // check if it's already in the que.
                    if (itemsQue.contains(location))
                        continue;

                    // check if its in the cache.
                    Node chunk = itemsCache.get(location);
                    
                    if (chunk != null)
                    {
                        // throw the TileLoaded event & check if the tile load has been cancelled.
                        if (!this.worldItemLoaded(chunk))
                            return false;

                        TerrainLodControl lodControl = new TerrainLodControl((TerrainQuad)chunk, app.getCamera());
                        lodControl.setExecutor(threadpool);
                        chunk.addControl(lodControl);

                        chunk.setShadowMode(ShadowMode.Receive);

                        app.getRootNode().attachChild(chunk);
                        app.getStateManager().getState(BulletAppState.class).getPhysicsSpace().add(chunk);
                        activeItems.put(location, chunk);

                        return true;
                    }
                    else
                    {
                        // its nowhere to be seen, generate it.
                        itemsQue.add(location);

                        threadpool.submit(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                Node newChunk = getWorldItem(location);
                                
                                if (newChunk != null)
                                {
                                    newItems.add(newChunk);

                                    // thread safety...
                                    app.enqueue(new Callable<Boolean>()
                                    {
                                        public Boolean call()
                                        {
                                            itemsQue.remove(location);
                                            return true;
                                        }
                                    });
                                }
                                
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
        itemsCache.clear();
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
                    final Node topChunk = getWorldItem(topLocation);

                    // bottom
                    final Vector3f bottomLocation = new Vector3f(x, 0, botRz + 1);
                    final Node bottomChunk = getWorldItem(bottomLocation);

                    app.enqueue(new Callable<Boolean>()
                    {
                        public Boolean call()
                        {
                            itemsCache.put(topLocation, topChunk);
                            itemsCache.put(bottomLocation, bottomChunk);

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
                    final Node leftChunk = getWorldItem(leftLocation);
                    leftChunk.setShadowMode(ShadowMode.Receive);

                    // right
                    final Vector3f rightLocation = new Vector3f(botRx + 1, 0, z);
                    final Node rightChunk = getWorldItem(rightLocation);
                    rightChunk.setShadowMode(ShadowMode.Receive);

                    app.enqueue(new Callable<Boolean>()
                    {
                        public Boolean call()
                        {
                            itemsCache.put(leftLocation, leftChunk);
                            itemsCache.put(rightLocation, rightChunk);

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
        
        int locX = (int)actualX >> this.getBitShiftCount();
        int locZ = (int)actualZ >> this.getBitShiftCount();
        
        locX /= worldScale;
        locZ /= worldScale;
        
        if ((locX == oldLocX) && (locZ == oldLocZ) && itemsQue.isEmpty() && newItems.isEmpty())
        {
            return;
        }
        
        topLx = locX - vd_west;
        topLz = locZ - vd_north;
        botRx = locX + vd_east;
        botRz = locZ + vd_south;
        
        if (checkForOldItems())
            return;
        
        if (checkForNewItems())
            return;
        
        if (itemsQue.isEmpty() && newItems.isEmpty())
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
