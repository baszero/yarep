package org.wyona.yarep.core;

import org.apache.avalon.framework.configuration.Configuration;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

/**
 *
 */
public interface Storage {

    /**
     *
     */
    public void readConfig(Configuration storageConfig, File repoConfigFile);

    /**
     *
     */
    public Writer getWriter(UID uid, Path path);

    /**
     *
     */
    public OutputStream getOutputStream(UID uid, Path path);

    /**
     *
     */
    public Reader getReader(UID uid, Path path) throws NoSuchNodeException;

    /**
     *
     */
    public InputStream getInputStream(UID uid, Path path) throws NoSuchNodeException;

    /**
     *
     */
    public long getLastModified(UID uid, Path path);
}
