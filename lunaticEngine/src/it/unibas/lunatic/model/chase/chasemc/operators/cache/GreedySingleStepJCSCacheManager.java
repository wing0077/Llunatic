package it.unibas.lunatic.model.chase.chasemc.operators.cache;

import it.unibas.lunatic.LunaticConfiguration;
import it.unibas.lunatic.model.chase.chasemc.CellGroup;
import it.unibas.lunatic.model.chase.chasemc.operators.IRunQuery;
import it.unibas.lunatic.model.database.CellRef;
import it.unibas.lunatic.model.database.IDatabase;
import it.unibas.lunatic.model.database.IValue;
import it.unibas.lunatic.persistence.relational.QueryStatManager;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import org.apache.jcs.JCS;
import org.apache.jcs.access.exception.CacheException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GreedySingleStepJCSCacheManager extends AbstractGreedyCacheManager {

    private static Logger logger = LoggerFactory.getLogger(GreedySingleStepJCSCacheManager.class);

    public static final String GROUPID = "standard";
    private final JCS cellGroupCache;
    private final JCS clusterIdCache;
    private Set<String> previousCachedStepIds = new HashSet<String>();
    private String currentCachedStepId;

    public GreedySingleStepJCSCacheManager(IRunQuery queryRunner) {
        super(queryRunner);
        try {
            this.cellGroupCache = JCS.getInstance("cellgroupcache");
            this.clusterIdCache = JCS.getInstance("clusteridcache");
        } catch (CacheException ex) {
            logger.error("Unable to create JCS Cache. " + ex.getLocalizedMessage());
            throw new IllegalStateException("Unable to create JCS Cache. " + ex.getLocalizedMessage());
        }
    }

    public CellGroup getCellGroup(IValue value, String stepId, IDatabase deltaDB) {
        loadCacheForStep(stepId, deltaDB);
        String key = buildKey(value, stepId);
        CellGroup cellGroup = (CellGroup) cellGroupCache.getFromGroup(key, GROUPID);
        return cellGroup;
    }

    public void putCellGroup(CellGroup cellGroup, String stepId, IDatabase deltaDB) {
        try {
            loadCacheForStep(stepId, deltaDB);
            String key = buildKey(cellGroup.getId(), stepId);
            cellGroupCache.putInGroup(key, GROUPID, cellGroup);
        } catch (CacheException ex) {
            logger.error("Unable to add objects to cache. " + ex.getLocalizedMessage());
            throw new IllegalStateException("Unable to add objects to cache. " + ex.getLocalizedMessage());
        }
    }

    public void removeCellGroup(IValue value, String stepId) {
        String key = buildKey(value, stepId);
        this.cellGroupCache.remove(key, GROUPID);
    }

    public IValue getClusterId(CellRef cellRef, String stepId, IDatabase deltaDB) {
        loadCacheForStep(stepId, deltaDB);
        String key = buildKey(cellRef, stepId);
        IValue value = (IValue) clusterIdCache.get(key);
        if (value == null) {
            return null;
        }
        return value;
    }

    public void putClusterId(CellRef cellRef, IValue value, String stepId, IDatabase deltaDB) {
        try {
            loadCacheForStep(stepId, deltaDB);
            String key = buildKey(cellRef, stepId);
            this.clusterIdCache.put(key, value);
        } catch (CacheException ex) {
            logger.error("Unable to add objects to cache. " + ex.getLocalizedMessage());
            throw new IllegalStateException("Unable to add objects to cache. " + ex.getLocalizedMessage());
        }
    }

    public void removeClusterId(CellRef cellRef, String stepId) {
        try {
            String key = buildKey(cellRef, stepId);
            this.clusterIdCache.remove(key);
        } catch (CacheException ex) {
            logger.error("Unable to remove objects to cache. " + ex.getLocalizedMessage());
            throw new IllegalStateException("Unable to remove objects to cache. " + ex.getLocalizedMessage());
        }
    }

    public void reset() {
        try {
            //New step to cache... cleaning old step
            cellGroupCache.clear();
            clusterIdCache.clear();
        } catch (CacheException ex) {
            logger.error("Unable to clear cache. " + ex.getLocalizedMessage());
            throw new IllegalStateException("Unable to clear cache. " + ex.getLocalizedMessage());
        }
    }

    @Override
    protected void loadCacheForStep(String stepId, IDatabase deltaDB) {
        if (stepId.equals(currentCachedStepId)) {
            return;
        }
        if (LunaticConfiguration.sout) System.out.println("\t******Loading cache for step: " + stepId + " (current step: " + currentCachedStepId + ")");
        if (previousCachedStepIds.contains(stepId)) {
            if (logger.isDebugEnabled()) logger.debug("Reloading an old cached step... " + stepId);
//            throw new IllegalArgumentException("Unable to reload a previous cached step.\n\tRequired Step: " + stepId + "\n\tCurrent cached step: " + currentCachedStepId + "\n\t" + previousCachedStepIds);
        }
        reset();
        if (logger.isDebugEnabled()) {
            logger.debug("### LOADING CACHE FOR STEP " + stepId);
            QueryStatManager.getInstance().printStatistics();
        }
        //LOAD CACHE
        currentCachedStepId = stepId;
        previousCachedStepIds.add(stepId);
        loadCellGroupsAndOccurrences(stepId, deltaDB);
        loadProvenances(stepId, deltaDB);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Set<String> getKeySet() {
        return cellGroupCache.getGroupKeys(GROUPID);
    }

    @Override
    protected CellGroup getCellGroup(String key) {
        return (CellGroup) this.cellGroupCache.getFromGroup(key, GROUPID);
    }
}

class CellRefStringComparator implements Comparator<CellRef> {

    public int compare(CellRef t, CellRef t1) {
        return t.toString().compareTo(t1.toString());
    }
}