package org.neo4j.jcr;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * The entry point into the Neo content repository.
 * 
 * @author Tobias Ivarsson (tobias.ivarsson@neotechnology.com)
 */
public class NeoRepository implements Repository {

    public String getDescriptor(String key) {
        // TODO Auto-generated method stub
        return null;
    }

    public String[] getDescriptorKeys() {
        // TODO Auto-generated method stub
        return null;
    }

    public Session login() throws LoginException, RepositoryException {
        // TODO Auto-generated method stub
        return null;
    }

    public Session login(Credentials credentials) throws LoginException,
            RepositoryException {
        // TODO Auto-generated method stub
        return null;
    }

    public Session login(String workspaceName) throws LoginException,
            NoSuchWorkspaceException, RepositoryException {
        // TODO Auto-generated method stub
        return null;
    }

    public Session login(Credentials credentials, String workspaceName) throws LoginException,
            NoSuchWorkspaceException, RepositoryException {
        // TODO Auto-generated method stub
        return null;
    }

}
