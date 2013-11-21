package org.wyona.yarep.impl.repo.vfs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.NumericField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;

import org.wyona.yarep.core.NoSuchRevisionException;
import org.wyona.yarep.core.Node;
import org.wyona.yarep.core.NodeStateException;
import org.wyona.yarep.core.NodeType;
import org.wyona.yarep.core.Path;
import org.wyona.yarep.core.Property;
import org.wyona.yarep.core.PropertyType;
import org.wyona.yarep.core.RepositoryException;
import org.wyona.yarep.core.Revision;
import org.wyona.yarep.core.UID;
import org.wyona.yarep.core.attributes.VersionableV1;
import org.wyona.yarep.impl.AbstractNode;
import org.wyona.yarep.impl.DefaultProperty;

/**
 * Utility class based on lucene to index and search revisions of a node by date (also see http://stackoverflow.com/questions/5495645/indexing-and-searching-date-in-lucene)
 */
public class DateIndexerSearcherImplV2 implements DateIndexerSearcher {
    private static Logger log = Logger.getLogger(DateIndexerSearcherImplV2.class);

    private File metaDir;
    private Node node;
    private File indexDir;

    private static final String CREATION_DATE_FIELD_NAME = "cdate";
    private static final String REVISION_NAME_FIELD_NAME = "rname";

    /**
     * @param node Node for revisions shall be indexed by date
     * @param metaDir Meta directory of this node
     */
    public DateIndexerSearcherImplV2(Node node, File metaDir) {
        this.node = node;
        this.metaDir = metaDir;
        indexDir = new File(metaDir, "lucene_index_data_utc");
    }

    /**
     * Check if date index already exists
     */
    public boolean indexExists() {
        return indexDir.isDirectory();
    }

    /**
     * @see org.wyona.yarep.impl.repo.vfs.DateIndexerSearcher#getRevisionOlderThan(Date)
     */
    public Revision getRevisionOlderThan(Date date) throws Exception {
        log.warn("DEBUG: Get revision older than: " + DateIndexerSearcherImplV1.format(date));
        Date olderThanDate = new Date(date.getTime() - 1);

        Revision revision = getRevision(olderThanDate);
        if (revision != null && date.getTime() > revision.getCreationDate().getTime()) {
            return revision;
        } else {
            log.warn("There seems to be NO revision older than: " + DateIndexerSearcherImplV1.format(date));
            return null;
        }
    }

    /**
     * @see org.wyona.yarep.impl.repo.vfs.DateIndexerSearcher#getMostRecentRevision()
     */
    public Revision getMostRecentRevision() {
        log.warn("DEBUG: Get most recent revision...");
        try {
            org.apache.lucene.search.Searcher searcher = new IndexSearcher(indexDir.getAbsolutePath());
            if (searcher != null) {
/*
                String q = "*:*";
                org.apache.lucene.search.Query query = new org.apache.lucene.queryParser.QueryParser(CREATION_DATE_FIELD_NAME, getAnalyzer()).parse(q);
*/
                org.apache.lucene.search.Query query = new org.apache.lucene.search.MatchAllDocsQuery();
                org.apache.lucene.search.Hits hits = searcher.search(query);
                log.warn("DEBUG: Query \"" + query + "\" on field '" + CREATION_DATE_FIELD_NAME + "' returned " + hits.length() + " hits");
                String revisionName = null;
                if (hits != null && hits.length() > 0) {
/* DEBUG
                    for (int i = 0; i < hits.length();i++) {
                        revisionName = hits.doc(i).getField(REVISION_NAME_FIELD_NAME).stringValue();
                        log.warn("DEBUG: Found revision name: " + revisionName);
                    }
*/
                    revisionName = hits.doc(hits.length() - 1).getField(REVISION_NAME_FIELD_NAME).stringValue();
                }
                searcher.close();
                if (revisionName != null) {
                    return node.getRevision(revisionName);
                } else {
                    return null;
                }
            } else {
                log.error("Searcher could not be initialized for index directory '" + indexDir + "'!");
                return null;
            }
        } catch(Exception e) {
            log.error(e, e);
            return null;
        }
    }

    /**
     * @see org.wyona.yarep.impl.repo.vfs.DateIndexerSearcher#getOldestRevision()
     */
    public Revision getOldestRevision() {
        try {
            // TODO: Find oldest year, set date to one year below oldest year and then use getRevisionOlderThan(Date date)
            log.warn("Implementation not finished yet!");
/*
            File dateIndexBaseDir = new File(this.metaDir, DATE_INDEX_BASE_DIR);
            String[] years = DateIndexerSearcherImplV1.sortAlphabeticallyAscending(dateIndexBaseDir.list());
            if (years != null && years.length > 0) {
                //log.debug("Year: " + years[years.length - 1]); // INFO: Descend, e.g. 2012, 2011, 2010, 2009, ...
                //return getRevisionFromIndexFile(getYoungestRevisionOfYear(new File(dateIndexBaseDir, years[years.length - 1])));
                return null; // TODO
            }
            log.warn("No year and hence no revision: " + dateIndexBaseDir);
*/
            return null;
        } catch(Exception e) {
            log.error(e, e);
            return null;
        }
    }

    /**
     * @see org.wyona.yarep.impl.repo.vfs.DateIndexerSearcher#getRevision(Date)
     */
    public Revision getRevision(Date date) throws Exception {
        log.warn("DEBUG: Get revision for date: " + DateIndexerSearcherImplV1.format(date));
        try {
            org.apache.lucene.search.Searcher searcher = new IndexSearcher(indexDir.getAbsolutePath());
            if (searcher != null) {
                org.apache.lucene.search.Query query = org.apache.lucene.search.NumericRangeQuery.newLongRange(CREATION_DATE_FIELD_NAME, new Long(0), new Long(date.getTime()), true, true);

                org.apache.lucene.search.Hits hits = searcher.search(query);
                log.warn("DEBUG: Query \"" + query + "\" on field '" + CREATION_DATE_FIELD_NAME + "' returned " + hits.length() + " hits");
                String revisionName = null;
                if (hits != null && hits.length() > 0) {
/*
                    for (int i = 0; i < hits.length();i++) {
                        revisionName = hits.doc(i).getField(REVISION_NAME_FIELD_NAME).stringValue();
                        log.warn("DEBUG: Found revision name: " + revisionName);
                    }
*/
                    revisionName = hits.doc(hits.length() - 1).getField(REVISION_NAME_FIELD_NAME).stringValue();
                }
                searcher.close();
                if (revisionName != null) {
                    return node.getRevision(revisionName);
                } else {
                    return null;
                }
            } else {
                log.error("Searcher could not be initialized for index directory '" + indexDir + "'!");
                return null;
            }
        } catch(Exception e) {
            log.error(e, e);
            return null;
        }
    }

    /**
     * Delete revision from date index
     * @param revisionName Name of revision to be deleted
     */
    public void deleteRevision(String revisionName) throws Exception {
        if (!indexExists()) {
            log.warn("No index yet, hence cannot delete revision '" + revisionName + "' from index.");
            return;
        }

/*
        File dateDirF = getRevisionDateDir(revisionName);
        File revisionIdFile = new File(dateDirF, DATE_INDEX_ID_FILENAME);
        if (revisionIdFile.isFile()) {
            String indexedRevisionName = getRevisionName(revisionIdFile.getAbsolutePath());
            if (indexedRevisionName.equals(revisionName)) {
                log.warn("Delete revision date/ID file: " + revisionIdFile);
                revisionIdFile.delete();
            } else {
                log.warn("Revision name '" + revisionName + "' is not equals revision name inside index file: " + revisionIdFile);
            }
        } else {
           log.warn("No such revision date file: " + revisionIdFile);
        }
*/
    }

    /**
     * @see org.wyona.yarep.impl.repo.vfs.DateIndexerSearcher#addRevision(String)
     */
    public void addRevision(String revisionName) throws Exception {
        if (!indexExists()) {
            buildDateIndex();
        }

        Date creationDate = node.getRevision(revisionName).getCreationDate(); // WARN: Older creation dates might not have milliseconds and hence are not corresponding exactly with the revision name, hence in order to build the date index correctly one needs to use the creation date
        log.warn("DEBUG: Add revision '" + revisionName + "' with creation date '" + creationDate + "' to date index ...");

        Document doc = new Document();
        doc.add(new NumericField(CREATION_DATE_FIELD_NAME, Field.Store.YES, true).setLongValue(creationDate.getTime()));
        //doc.add(new Field(CREATION_DATE_FIELD_NAME, org.apache.lucene.document.DateTools.dateToString(creationDate, org.apache.lucene.document.DateTools.Resolution.MILLISECOND), Field.Store.YES, Field.Index.NOT_ANALYZED));
        doc.add(new Field(REVISION_NAME_FIELD_NAME, revisionName, Field.Store.YES, Field.Index.NOT_ANALYZED));

        IndexWriter iw = getIndexWriter();
        Term revisionNameTerm = new Term(REVISION_NAME_FIELD_NAME, revisionName);
        iw.updateDocument(revisionNameTerm, doc);
        iw.optimize();
        iw.close();
    }

    /**
     * Build date index in order to retrieve revisions more quickly based on creation date
     */
    public void buildDateIndex() throws Exception {
/*
        File dateIndexBaseDir = new File(this.metaDir, DATE_INDEX_BASE_DIR);

        if (!dateIndexBaseDir.isDirectory()) {
            dateIndexBaseDir.mkdirs();
        }

        log.warn("Build date index '" + dateIndexBaseDir + "', whereas this should happen only once when no index exists yet (or has been manually deleted again). Please note that the reading of the revisions must be based on the implementation VirtualFileSystemNode#readRevisions()!");
        Revision[] revisions = node.getRevisions();
        for (int i = revisions.length - 1; i >= 0; i--) {
            addRevision(revisions[i].getRevisionName());
        }
*/
    }

    /**
     * Get lucene index writer
     */
    private IndexWriter getIndexWriter() throws Exception {
        IndexWriter iw = null;
        org.apache.lucene.analysis.Analyzer analyzer = getAnalyzer();
        if (indexDir.isDirectory()) {
           try {
               iw = new IndexWriter(indexDir.getAbsolutePath(), analyzer, false);
           } catch (FileNotFoundException e) {
               log.warn("Index directory '" + indexDir.getAbsolutePath() + "' seems to exist, but probably no segment files yet!");
               iw = new IndexWriter(indexDir.getAbsolutePath(), analyzer, true);
           }
        } else {
            indexDir.mkdirs();
            iw = new IndexWriter(indexDir.getAbsolutePath(), analyzer, true);
        }
        return iw;
    }

    /**
     * Get analyzer
     * @return analyzer
     */
    private org.apache.lucene.analysis.Analyzer getAnalyzer() {
        return new org.apache.lucene.analysis.standard.StandardAnalyzer();
        //return new org.apache.lucene.analysis.WhitespaceAnalyzer();
    }
}
