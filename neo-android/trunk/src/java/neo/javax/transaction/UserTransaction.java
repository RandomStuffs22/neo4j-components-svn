package neo.javax.transaction;

public interface UserTransaction {
	  public abstract void begin() throws NotSupportedException, SystemException;
	  
	  public abstract void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException,IllegalStateException, SystemException;
	  
	  public abstract int getStatus() throws SystemException;
	  
	  // Method descriptor #11 ()V
	  public abstract void rollback() throws java.lang.IllegalStateException, java.lang.SecurityException, SystemException;
	  
	  // Method descriptor #11 ()V
	  public abstract void setRollbackOnly() throws IllegalStateException, SystemException;
	  
	  // Method descriptor #12 (I)V
	  public abstract void setTransactionTimeout(int arg0) throws SystemException;
}
