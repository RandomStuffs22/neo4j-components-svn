package neo.javax.transaction;

import neo.javax.transaction.xa.XAResource;



public interface Transaction {

	public boolean enlistResource(XAResource resource) throws RollbackException, IllegalStateException, SystemException;

	public boolean delistResource(XAResource resource, int i) throws SystemException, RollbackException;
	
	public void setRollbackOnly();

	public void registerSynchronization(Synchronization evaluator)  throws SystemException, RollbackException;
	
	public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, IllegalStateException, SystemException;
	
	public void rollback() throws IllegalStateException, SystemException;
	
}
