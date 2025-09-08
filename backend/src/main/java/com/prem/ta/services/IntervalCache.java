package com.prem.ta.services;

import com.prem.ta.entities.Interval;
import com.prem.ta.repositories.IntervalRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class IntervalCache {

    private final IntervalRepository intervalRepository;
    private final Map<String, Interval> cache = new ConcurrentHashMap<>();

    public IntervalCache(IntervalRepository intervalRepository) {
        this.intervalRepository = intervalRepository;
    }

    /**
     * Load all intervals from the database into the cache at startup.
     */
    @PostConstruct
    public void init() {
        loadIntervals();
    }

    /**
     * Populate the cache from the DB.
     */
    public void loadIntervals() {
        List<Interval> intervals = intervalRepository.findAll();
        if (intervals.isEmpty()) {
            throw new IllegalStateException("No intervals found in DB! Please seed the intervals table.");
        }
        intervals.forEach(interval -> cache.put(interval.getLabel(), interval));
    }

    /**
     * Get interval by label.
     *
     * @param label the interval label (e.g., "1d")
     * @return the Interval entity
     */
    public Interval get(String label) {
        Interval interval = cache.get(label);
        if (interval == null) {
            throw new IllegalArgumentException("Interval not found in cache: " + label + ". Ensure it exists in DB and cache is loaded.");
        }
        return interval;
    }

    /**
     * Optional: refresh cache dynamically if DB changes.
     */
    public void refresh() {
        cache.clear();
        loadIntervals();
    }
}
