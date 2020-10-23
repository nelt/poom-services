package org.codingmatters.poom.caches.in.memory;

import org.codingmatters.poom.caches.in.memory.stores.MapCacheStore;
import org.codingmatters.poom.caches.invalidation.Invalidation;
import org.codingmatters.poom.services.tests.Eventually;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Test;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.contains;

public class CacheManagerTest {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final CacheWithStore<String, String> cache = new CacheWithStore<>(key -> key, new MapCacheStore<>(), (key, value) -> Invalidation.valid());
    private final CacheManager<String, String> manager = new CacheManager<>(
            cache,
            5,
            this.scheduler,
            1, TimeUnit.SECONDS
    );

    @After
    public void tearDown() throws Exception {
        this.manager.close();
        this.scheduler.shutdownNow();
    }

    @Test
    public void leastRecentlyAccessedKeysArePrunedWhenCapacityExceede() throws Exception {
        List<String> pruned = Collections.synchronizedList(new LinkedList<>());
        this.cache.addPruneListener(key -> pruned.add(key));

        for (int i = 0; i < 10; i++) {
            this.cache.get("key" + i);
        }

        Eventually.defaults().assertThat( () -> pruned, contains("key4", "key3", "key2", "key1", "key0"));
    }
}