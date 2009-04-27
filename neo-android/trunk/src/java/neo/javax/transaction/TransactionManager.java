package neo.javax.transaction;

import javax.transaction.InvalidTransactionException;


public interface TransactionManager {

	void begin() throws NotSupportedException, SystemException;

	Transaction getTransaction() throws SystemException;

	void setRollbackOnly() throws SystemException;

	int getStatus() throws SystemException;

	void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException;

	void rollback() throws IllegalStateException, SecurityException, SystemException;

	void resume(Transaction tx) throws InvalidTransactionException, IllegalStateException, SystemException;

	void setTransactionTimeout(int sec) throws SystemException;

	Transaction suspend() throws SystemException;

}
