package services.system;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.text.SimpleDateFormat;

/**
 * Thread-safe LRU (Least Recently Used) Cache implementation.
 * 
 * Features:
 * - ConcurrentHashMap for thread-safe operations
 * - LRU eviction policy
 * - Maximum cache size (150 entries)
 * - Cache statistics (hit rate, miss rate, eviction count)
 * - Memory usage tracking
 * - Cache warming support
 * - Manual cache clear
 */
public class LRUCache<K, V> {
    
    private final int maxSize;
    private final ConcurrentHashMap<K, CacheEntry<V>> cache;
    private final AtomicInteger hits = new AtomicInteger(0);
    private final AtomicInteger misses = new AtomicInteger(0);
    private final AtomicInteger evictions = new AtomicInteger(0);
    private final AtomicLong totalHitTime = new AtomicLong(0);
    private final AtomicLong totalMissTime = new AtomicLong(0);
    
    /**
     * Cache entry with access timestamp for LRU eviction.
     */
    private static class CacheEntry<V> {
        private final V value;
        private volatile long lastAccessTime;
        private final long creationTime;
        
        public CacheEntry(V value) {
            this.value = value;
            this.lastAccessTime = System.currentTimeMillis();
            this.creationTime = System.currentTimeMillis();
        }
        
        public V getValue() { return value; }
        public long getLastAccessTime() { return lastAccessTime; }
        public void updateAccessTime() { this.lastAccessTime = System.currentTimeMillis(); }
        public long getCreationTime() { return creationTime; }
    }
    
    public LRUCache(int maxSize) {
        this.maxSize = maxSize;
        this.cache = new ConcurrentHashMap<>(maxSize);
    }
    
    /** Gets a value from the cache in O(1) average time and updates access time for LRU tracking. */
    public V get(K key) {
        long startTime = System.nanoTime();
        CacheEntry<V> entry = cache.get(key);
        
        if (entry != null) {
            // Cache hit
            entry.updateAccessTime();
            long duration = System.nanoTime() - startTime;
            totalHitTime.addAndGet(duration);
            hits.incrementAndGet();
            return entry.getValue();
        } else {
            // Cache miss
            long duration = System.nanoTime() - startTime;
            totalMissTime.addAndGet(duration);
            misses.incrementAndGet();
            return null;
        }
    }
    
    /** Inserts or replaces a value (avg O(1)); evicts one least-recently-used entry in O(n) when full. */
    public void put(K key, V value) {
        synchronized (cache) {
            // Check if we need to evict
            if (cache.size() >= maxSize && !cache.containsKey(key)) {
                evictLRU();
            }
            
            cache.put(key, new CacheEntry<>(value));
        }
    }
    
    /** Scans entries to remove the least recently used key (O(n) over current cache size). */
    private void evictLRU() {
        K lruKey = null;
        long oldestAccessTime = Long.MAX_VALUE;
        
        // Find least recently used entry
        for (Map.Entry<K, CacheEntry<V>> entry : cache.entrySet()) {
            long accessTime = entry.getValue().getLastAccessTime();
            if (accessTime < oldestAccessTime) {
                oldestAccessTime = accessTime;
                lruKey = entry.getKey();
            }
        }
        
        if (lruKey != null) {
            cache.remove(lruKey);
            evictions.incrementAndGet();
        }
    }
    
    /**
     * Invalidates cache entry (removes from cache).
     */
    public void invalidate(K key) {
        cache.remove(key);
    }
    
    /**
     * Clears entire cache.
     */
    public void clear() {
        cache.clear();
        hits.set(0);
        misses.set(0);
        evictions.set(0);
        totalHitTime.set(0);
        totalMissTime.set(0);
    }
    
    /**
     * Gets cache statistics.
     */
    public Map<String, Object> getStatistics() {
        int totalRequests = hits.get() + misses.get();
        double hitRate = totalRequests > 0 ? (hits.get() * 100.0 / totalRequests) : 0.0;
        double missRate = totalRequests > 0 ? (misses.get() * 100.0 / totalRequests) : 0.0;
        
        long avgHitTime = hits.get() > 0 ? totalHitTime.get() / hits.get() : 0;
        long avgMissTime = misses.get() > 0 ? totalMissTime.get() / misses.get() : 0;
        
        // Estimate memory usage (rough calculation)
        long estimatedMemory = estimateMemoryUsage();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("hitRate", hitRate);
        stats.put("missRate", missRate);
        stats.put("averageHitTime", avgHitTime);
        stats.put("averageMissTime", avgMissTime);
        stats.put("totalEntries", cache.size());
        stats.put("maxSize", maxSize);
        stats.put("memoryUsage", estimatedMemory);
        stats.put("evictionCount", evictions.get());
        stats.put("totalHits", hits.get());
        stats.put("totalMisses", misses.get());
        
        return stats;
    }
    
    /**
     * Estimates memory usage of cache (rough calculation).
     */
    private long estimateMemoryUsage() {
        // Rough estimate: each entry ~100 bytes overhead + value size
        return cache.size() * 100; // Simplified estimation
    }
    
    /**
     * Displays cache contents with access timestamps.
     */
    public void displayCacheContents() {
        System.out.println("\n╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                         CACHE CONTENTS                                  ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝");
        System.out.println();
        
        if (cache.isEmpty()) {
            System.out.println("Cache is empty.");
            return;
        }
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        // Sort by last access time (most recent first)
        List<Map.Entry<K, CacheEntry<V>>> sortedEntries = new ArrayList<>(cache.entrySet());
        sortedEntries.sort((a, b) -> Long.compare(
            b.getValue().getLastAccessTime(),
            a.getValue().getLastAccessTime()
        ));
        
        for (Map.Entry<K, CacheEntry<V>> entry : sortedEntries) {
            System.out.println("┌─ Entry ─────────────────────────────────────────────────────────────┐");
            System.out.printf("│ Key: %-58s │%n", entry.getKey());
            System.out.printf("│ Last Access: %-50s │%n", 
                sdf.format(new Date(entry.getValue().getLastAccessTime())));
            System.out.printf("│ Created: %-54s │%n", 
                sdf.format(new Date(entry.getValue().getCreationTime())));
            System.out.println("└────────────────────────────────────────────────────────────────────────┘");
        }
    }
    
    /**
     * Warms cache with provided entries.
     */
    public void warmCache(Map<K, V> entries) {
        for (Map.Entry<K, V> entry : entries.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }
    
    /**
     * Gets current cache size.
     */
    public int size() {
        return cache.size();
    }
    
    /**
     * Checks if cache contains key.
     */
    public boolean containsKey(K key) {
        return cache.containsKey(key);
    }
}

