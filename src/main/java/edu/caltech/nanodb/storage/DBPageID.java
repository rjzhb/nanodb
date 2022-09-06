package edu.caltech.nanodb.storage;


import java.io.File;


/**
 * A class representing the unique identity of a {@code DBPage}, used as
 * a key for tracking pages in maps.
 */
public class DBPageID {
    private File file;

    private int pageNo;

    public DBPageID(File file, int pageNo) {
        this.file = file;
        this.pageNo = pageNo;
    }

    public DBPageID(DBPage dbPage) {
        this(dbPage.getDBFile().getDataFile(), dbPage.getPageNo());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DBPageID) {
            DBPageID other = (DBPageID) obj;
            return file.equals(other.file) && pageNo == other.pageNo;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = hash * 37 + file.hashCode();
        hash = hash * 37 + pageNo;
        return hash;
    }
}
