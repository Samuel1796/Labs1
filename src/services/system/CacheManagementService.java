package services.system;

import utilities.CacheUtils;
import java.util.Map;

/**
 * Service for managing and displaying cache information.
 */
public class CacheManagementService {
    
    /**
     * Displays comprehensive cache management information.
     */
    public static void displayCacheManagement() {
        System.out.println("\n=======================================================================");
        System.out.println("                         CACHE MANAGEMENT                             ");
        System.out.println("=======================================================================");
        System.out.println();
        
        System.out.println("Cache Storage Information:");
        System.out.println("-----------------------------------------------------------------------");
        System.out.println("Cache Type: In-Memory LRU Cache");
        System.out.println("Cache Directory: Memory (no file storage)");
        System.out.println("Cache Configuration: Managed by CacheUtils");
        System.out.println("Default Cache Size: 150 entries per cache");
        System.out.println();
        
        System.out.println("Available Caches:");
        System.out.println("-----------------------------------------------------------------------");
        System.out.println("1. Statistics Cache - Stores computed statistics");
        System.out.println("2. Student Cache - Stores student data");
        System.out.println("3. Performance Cache - Stores performance metrics");
        System.out.println();
        
        CacheUtils.displayAllCacheStatistics();
        
        System.out.println("Cache Operations:");
        System.out.println("-----------------------------------------------------------------------");
        System.out.println("• Cache entries are stored in memory for fast access");
        System.out.println("• LRU (Least Recently Used) eviction policy");
        System.out.println("• Automatic eviction when cache reaches maximum size");
        System.out.println("• Thread-safe concurrent access");
        System.out.println("• Real-time statistics tracking");
        System.out.println();
    }
    
    /**
     * Gets real-time cache statistics.
     */
    public static Map<String, Map<String, Object>> getRealTimeCacheStatistics() {
        return CacheUtils.getAllCacheStatistics();
    }
}

