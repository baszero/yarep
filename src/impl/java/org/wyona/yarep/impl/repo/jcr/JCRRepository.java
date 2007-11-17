package org.wyona.yarep.impl.repo.jcr;

import org.wyona.yarep.core.Node;
import org.wyona.yarep.core.NoSuchNodeException;
import org.wyona.yarep.core.Path;
import org.wyona.yarep.core.Repository;
import org.wyona.yarep.core.RepositoryException;
import org.wyona.yarep.core.UID;

import org.wyona.commons.io.FileUtil;

import org.apache.log4j.Category;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import org.apache.jackrabbit.core.TransientRepository;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.DefaultConfigurationBuilder;

/**
 * JCR based repository.
 */
public class JCRRepository implements Repository {

    private static Category log = Category.getInstance(JCRRepository.class);

    private String repoId;
    private String repoName;
    private String jcrRepoDesc;
    private File yarepConfigFile;
    private File jackrabbitConfigFile;

    /**
     *
     */
    public JCRRepository() {
        log.error("DEBUG: Create new Yarep-JCR repo ...");
    }

    /**
     *
     */
    public void move(String srcPath, String destPath) throws RepositoryException {
        log.error("Not implemented yet!");
    }
    
    /**
     *
     */
    public void copy(String srcPath, String destPath) throws RepositoryException {
        log.error("Not implemented yet!");
    }

    /**
     *
     */
    public Node getRootNode() throws RepositoryException {
        log.error("Not implemented yet!");
        return null;
    }

    /**
     *
     */
    public boolean existsNode(String path) throws RepositoryException {
        log.error("Not implemented yet!");
        return true;
    }

    /**
     *
     */
    public Node getNodeByUUID(String uuid) throws NoSuchNodeException, RepositoryException {
        log.error("Not implemented yet!");
        return null;
    }

    /**
     *
     */
    public Node getNode(String path) throws NoSuchNodeException, RepositoryException {
        log.error("Not implemented yet!");
        return null;
    }
   
    /**
     *
     */
    public void addSymbolicLink(Path target, Path link) throws RepositoryException {
        log.error("Not implemented yet!");
    }

    /**
     *
     */
    public String[] getRevisions(Path path) throws RepositoryException {
        log.error("Not implemented yet!");
        return null;
    }

    /**
     *
     */
    public UID getUID(Path path) throws RepositoryException {
        log.error("Not implemented yet!");
        return null;
    }

    /**
     *
     */
    public Path[] getChildren(Path path) throws RepositoryException {
        log.error("Not implemented yet!");
        return null;
    }

    /**
     *
     */
    public boolean exists(Path path) throws RepositoryException {
        return existsNode(path.toString());
    }

    /**
     *
     */
    public boolean isCollection(Path path) throws RepositoryException {
        return getNode(path.toString()).isCollection();
    }

    /**
     *
     */
    public boolean isResource(Path path) throws RepositoryException {
        return getNode(path.toString()).isResource();
    }

    /**
     *
     */
    public void getURI(Path path) throws RepositoryException {
        log.error("Not implemented yet!");
    }

    /**
     *
     */
    public void getContentLength(Path path) throws RepositoryException {
        log.error("Not implemented yet!");
    }

    /**
     *
     */
    public void getValidity(Path path) throws RepositoryException {
        log.error("Not implemented yet!");
    }

    /**
     *
     */
    public boolean delete(Path path) throws RepositoryException {
        getNode(path.toString()).delete();
        return true;
    }

    /**
     *
     */
    public boolean delete(Path path, boolean recursive) throws RepositoryException {
        if (recursive) throw new RepositoryException("Not implemented yet!");
        return delete(path);
    }

    /**
     *
     */
    public long getSize(Path path) throws RepositoryException {
        return getNode(path.toString()).getSize();
    }

    /**
     *
     */
    public long getLastModified(Path path) throws RepositoryException {
        return getNode(path.toString()).getLastModified();
    }

    /**
     *
     */
    public InputStream getInputStream(Path path) throws RepositoryException {
        return getNode(path.toString()).getInputStream();
    }

    /**
     *
     */
    public OutputStream getOutputStream(Path path) throws RepositoryException {
        return getNode(path.toString()).getOutputStream();
    }

    /**
     *
     */
    public Reader getReader(Path path) throws RepositoryException {
        try {
            return new InputStreamReader(getNode(path.toString()).getInputStream(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RepositoryException(e.getMessage(), e);
        }
    }

    /**
     *
     */
    public Writer getWriter(Path path) throws RepositoryException {
        try {
            return new OutputStreamWriter(getNode(path.toString()).getOutputStream(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RepositoryException(e.getMessage(), e);
        }
    }

    /**
     *
     */
    public File getConfigFile() {
        return yarepConfigFile;
    }

    /**
     *
     */
    public String getName() {
        return repoName + "(" + jcrRepoDesc + ")";
    }

    /**
     * Like an init() method ...
     */
    public void readConfiguration(File configFile) throws RepositoryException {
        yarepConfigFile = configFile;
        DefaultConfigurationBuilder builder = new DefaultConfigurationBuilder();
        Configuration config;

        try {
            config = builder.buildFromFile(yarepConfigFile);

            // Read repo name
            repoName = config.getChild("name", false).getValue();

            // Read jackrabbit config file
            jackrabbitConfigFile = new File(config.getChild("jackrabbit-repository-config", false).getAttribute("src"));
            if (!jackrabbitConfigFile.isAbsolute()) {
                jackrabbitConfigFile = FileUtil.file(yarepConfigFile.getParent(), jackrabbitConfigFile.toString());
            }
            log.error("DEBUG: Jackrabbit config: " + jackrabbitConfigFile);
            System.setProperty("org.apache.jackrabbit.repository.conf", jackrabbitConfigFile.toString());
            System.setProperty("org.apache.jackrabbit.repository.home", jackrabbitConfigFile.getParent());

        } catch (Exception e) {
            log.error(e, e);
            throw new RepositoryException("Could not read repository configuration: " + e.getMessage(), e);
        }

        try {
            javax.jcr.Repository repository = new TransientRepository();

            javax.jcr.Session session = repository.login();
            try {
                String user = session.getUserID();
                String jcrRepoDesc = repository.getDescriptor(javax.jcr.Repository.REP_NAME_DESC);
                log.error("DEBUG: Logged in as " + user + " to a " + jcrRepoDesc + " repository.");

                javax.jcr.Node root = session.getRootNode();
                javax.jcr.Node testNode = root.addNode("my-test-node");
                testNode.setProperty("description", "Test Hugo");
                session.save();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            } finally {
                session.logout();
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     *
     */
    public String getID() {
        return repoId;
    }

    /**
     *
     */
    public void setID(String id) {
        repoId = id;
    }

    /**
     *
     */
    public String toString() {
        return "JCR Wrapper Repository: ID = " + getID();
    }
}
