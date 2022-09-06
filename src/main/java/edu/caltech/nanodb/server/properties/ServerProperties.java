package edu.caltech.nanodb.server.properties;


import java.io.File;

import edu.caltech.nanodb.storage.DBFile;

public interface ServerProperties {

    /**
     * The system property that specifies where all of the database's files
     * will be stored.
     */
    String PROP_BASE_DIRECTORY = "nanodb.baseDirectory";

    /**
     * This is the default value of the base-directory property,
     * <tt>./datafiles</tt> (or <tt>.\datafiles</tt> on Windows).
     */
    String DEFAULT_BASE_DIRECTORY = "." + File.separatorChar + "datafiles";

    // --- PAGE CACHE PROPERTIES ---------------------------------------------

    /**
     * The system property that can be used to specify the size of the page
     * cache in the buffer manager.
     */
    String PROP_PAGECACHE_SIZE = "nanodb.pagecache.size";

    /**
     * This is the minimum page-cache size allowed.  While it might be fun
     * to allow smaller cache sizes, we should probably make sure that we can
     * at least hold one page of the maximal size supported by NanoDB.
     */
    int MIN_PAGECACHE_SIZE = DBFile.MAX_PAGESIZE;

    /** This is the maximum page-cache size allowed, 1GiB. */
    int MAX_PAGECACHE_SIZE = 1 << 30;

    /** The default page-cache size is 1MiB. */
    int DEFAULT_PAGECACHE_SIZE = 1 << 20;

    String PROP_PAGECACHE_POLICY = "nanodb.pagecache.policy";

    String[] PAGECACHE_POLICY_VALUES = {"FIFO", "LRU"};

    String DEFAULT_PAGECACHE_POLICY = "LRU";


    /**
     * The system property that can be used to specify the default page-size
     * to use when creating new database files.
     */
    String PROP_PAGE_SIZE = "nanodb.pagesize";

    int DEFAULT_PAGE_SIZE = 8192;

    // --- CONSTRAINT PROPERTIES ---------------------------------------------

    String PROP_ENFORCE_KEY_CONSTRAINTS = "nanodb.enforceKeyConstraints";

    // --- INDEX PROPERTIES --------------------------------------------------

    /**
     * The system property that can be used to turn on or off indexes.
     */
    public static final String PROP_ENABLE_INDEXES = "nanodb.enableIndexes";


    /**
     * The name of the property to enable or disable the "create indexes on
     * keys" functionality in "<tt>CREATE TABLE ...</tt>".
     */
    public static final String PROP_CREATE_INDEXES_ON_KEYS =
        "nanodb.createIndexesOnKeys";




    /**
     * The system property that can be used to turn on or off transaction
     * processing.
     */
    public static final String PROP_ENABLE_TRANSACTIONS =
        "nanodb.enableTransactions";


    /**
     * The name of the property to enable or disable the "flush data after
     * each command" functionality.
     */
    public static final String PROP_FLUSH_AFTER_CMD = "nanodb.flushAfterCmd";


    /**
     * This property can be used to specify a different query-planner class
     * for NanoDB to use.
     */
    public static final String PROP_PLANNER_CLASS = "nanodb.plannerClass";


    /**
     * This class is the default planner used in NanoDB, unless
     * overridden in the configuration.
     */
    public static final String DEFAULT_PLANNER_CLASS =
        "edu.caltech.nanodb.queryeval.SimplestPlanner";
}
