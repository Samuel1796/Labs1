package utilities;

import services.system.LRUCache;
import java.util.*;
import java.util.concurrent.*;

/**
 * Centralized cache management utility with executor framework management.
 */
public class CacheUtils {
    
    private static final int DEFAULT_CACHE_SIZE = 150;
    private static final int DEFAULT_FIXED_POOL_SIZE = 10;
    private static final int DEFAULT_SCHEDULED_POOL_SIZE = 5;
    
    private static final LRUCache<String, Object> statisticsCache = new LRUCache<>(DEFAULT_CACHE_SIZE);
    private static final LRUCache<String, Object> studentCache = new LRUCache<>(DEFAULT_CACHE_SIZE);
    private static final LRUCache<String, Object> performanceCache = new LRUCache<>(DEFAULT_CACHE_SIZE);
    
    private static final ConcurrentHashMap<String, LRUCache<?, ?>> cacheRegistry = new ConcurrentHashMap<>();
    
    private static ExecutorService fixedThreadPool;
    private static ExecutorService cachedThreadPool;
    private static ScheduledExecutorService scheduledThreadPool;
    
    static {
        cacheRegistry.put("statistics", statisticsCache);
        cacheRegistry.put("student", studentCache);
        cacheRegistry.put("performance", performanceCache);
        initializeExecutors();
    }
    
    private static void initializeExecutors() {
        fixedThreadPool = Executors.newFixedThreadPool(DEFAULT_FIXED_POOL_SIZE, r -> {
            Thread t = new Thread(r, "CacheUtils-FixedPool");
            t.setDaemon(true);
            return t;
        });
        
        cachedThreadPool = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "CacheUtils-CachedPool");
            t.setDaemon(true);
            return t;
        });
        
        scheduledThreadPool = Executors.newScheduledThreadPool(DEFAULT_SCHEDULED_POOL_SIZE, r -> {
            Thread t = new Thread(r, "CacheUtils-ScheduledPool");
            t.setDaemon(true);
            return t;
        });
    }
    
    /**
     * Gets the statistics cache instance.
     */
    public static LRUCache<String, Object> getStatisticsCache() {
        return statisticsCache;
    }
    
    /**
     * Gets the student data cache instance.
     */
    public static LRUCache<String, Object> getStudentCache() {
        return studentCache;
    }
    
    /**
     * Gets the performance metrics cache instance.
     */
    public static LRUCache<String, Object> getPerformanceCache() {
        return performanceCache;
    }
    
    /**
     * Creates a new named cache and registers it.
     */
    public static <K, V> LRUCache<K, V> createCache(String name, int maxSize) {
        LRUCache<K, V> cache = new LRUCache<>(maxSize);
        cacheRegistry.put(name, cache);
        return cache;
    }
    
    /**
     * Gets a registered cache by name.
     */
    @SuppressWarnings("unchecked")
    public static <K, V> LRUCache<K, V> getCache(String name) {
        return (LRUCache<K, V>) cacheRegistry.get(name);
    }
    
    /**
     * Invalidates a specific key in a named cache.
     */
    @SuppressWarnings("unchecked")
    public static void invalidate(String cacheName, String key) {
        LRUCache<?, ?> cache = cacheRegistry.get(cacheName);
        if (cache != null) {
            ((LRUCache<String, Object>) cache).invalidate(key);
        }
    }
    
    /**
     * Clears a specific cache by name.
     */
    public static void clearCache(String cacheName) {
        LRUCache<?, ?> cache = cacheRegistry.get(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }
    
    /**
     * Clears all registered caches.
     */
    public static void clearAllCaches() {
        for (LRUCache<?, ?> cache : cacheRegistry.values()) {
            cache.clear();
        }
    }
    
    /**
     * Gets statistics for a specific cache.
     */
    public static Map<String, Object> getCacheStatistics(String cacheName) {
        LRUCache<?, ?> cache = cacheRegistry.get(cacheName);
        if (cache != null) {
            return cache.getStatistics();
        }
        return Collections.emptyMap();
    }
    
    /**
     * Gets statistics for all registered caches.
     */
    public static Map<String, Map<String, Object>> getAllCacheStatistics() {
        Map<String, Map<String, Object>> allStats = new HashMap<>();
        for (Map.Entry<String, LRUCache<?, ?>> entry : cacheRegistry.entrySet()) {
            allStats.put(entry.getKey(), entry.getValue().getStatistics());
        }
        return allStats;
    }
    
    /**
     * Displays statistics for all caches.
     */
    public static void displayAllCacheStatistics() {
        System.out.println("\n=======================================================================");
        System.out.println("                         CACHE STATISTICS                                ");
        System.out.println("==========================================================================");
        System.out.println();
        
        Map<String, Map<String, Object>> allStats = getAllCacheStatistics();
        
        if (allStats.isEmpty()) {
            System.out.println("No caches registered.");
            return;
        }
        
        for (Map.Entry<String, Map<String, Object>> entry : allStats.entrySet()) {
            String cacheName = entry.getKey();
            Map<String, Object> stats = entry.getValue();
            
            System.out.println("Cache: " + cacheName);
            System.out.println("-----------------------------------------------------------------------");
            System.out.printf("  Hit Rate: %.2f%%%n", stats.get("hitRate"));
            System.out.printf("  Miss Rate: %.2f%%%n", stats.get("missRate"));
            System.out.printf("  Total Entries: %d%n", stats.get("totalEntries"));
            System.out.printf("  Max Size: %d%n", stats.get("maxSize"));
            System.out.printf("  Total Hits: %d%n", stats.get("totalHits"));
            System.out.printf("  Total Misses: %d%n", stats.get("totalMisses"));
            System.out.printf("  Evictions: %d%n", stats.get("evictionCount"));
            System.out.println();
        }
    }
    
    /**
     * Gets the fixed thread pool executor.
     */
    public static ExecutorService getFixedThreadPool() {
        return fixedThreadPool;
    }
    
    /**
     * Gets the cached thread pool executor.
     */
    public static ExecutorService getCachedThreadPool() {
        return cachedThreadPool;
    }
    
    /**
     * Gets the scheduled thread pool executor.
     */
    public static ScheduledExecutorService getScheduledThreadPool() {
        return scheduledThreadPool;
    }
    
    /**
     * Gets statistics for all thread pools.
     */
    public static Map<String, Map<String, Object>> getThreadPoolStatistics() {
        Map<String, Map<String, Object>> stats = new HashMap<>();
        
        if (fixedThreadPool instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor tpe = (ThreadPoolExecutor) fixedThreadPool;
            Map<String, Object> fixedStats = new HashMap<>();
            fixedStats.put("poolSize", tpe.getPoolSize());
            fixedStats.put("activeCount", tpe.getActiveCount());
            fixedStats.put("queueSize", tpe.getQueue().size());
            fixedStats.put("completedTaskCount", tpe.getCompletedTaskCount());
            fixedStats.put("corePoolSize", tpe.getCorePoolSize());
            fixedStats.put("maximumPoolSize", tpe.getMaximumPoolSize());
            stats.put("fixedThreadPool", fixedStats);
        }
        
        if (cachedThreadPool instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor tpe = (ThreadPoolExecutor) cachedThreadPool;
            Map<String, Object> cachedStats = new HashMap<>();
            cachedStats.put("poolSize", tpe.getPoolSize());
            cachedStats.put("activeCount", tpe.getActiveCount());
            cachedStats.put("queueSize", tpe.getQueue().size());
            cachedStats.put("completedTaskCount", tpe.getCompletedTaskCount());
            cachedStats.put("corePoolSize", tpe.getCorePoolSize());
            cachedStats.put("maximumPoolSize", tpe.getMaximumPoolSize());
            stats.put("cachedThreadPool", cachedStats);
        }
        
        if (scheduledThreadPool instanceof ScheduledThreadPoolExecutor) {
            ScheduledThreadPoolExecutor stpe = (ScheduledThreadPoolExecutor) scheduledThreadPool;
            Map<String, Object> scheduledStats = new HashMap<>();
            scheduledStats.put("poolSize", stpe.getPoolSize());
            scheduledStats.put("activeCount", stpe.getActiveCount());
            scheduledStats.put("queueSize", stpe.getQueue().size());
            scheduledStats.put("completedTaskCount", stpe.getCompletedTaskCount());
            scheduledStats.put("corePoolSize", stpe.getCorePoolSize());
            scheduledStats.put("maximumPoolSize", stpe.getMaximumPoolSize());
            stats.put("scheduledThreadPool", scheduledStats);
        }
        
        return stats;
    }
    
    /**
     * Shuts down all thread pools gracefully.
     */
    public static void shutdownExecutors() {
        if (fixedThreadPool != null && !fixedThreadPool.isShutdown()) {
            fixedThreadPool.shutdown();
        }
        if (cachedThreadPool != null && !cachedThreadPool.isShutdown()) {
            cachedThreadPool.shutdown();
        }
        if (scheduledThreadPool != null && !scheduledThreadPool.isShutdown()) {
            scheduledThreadPool.shutdown();
        }
    }
}

