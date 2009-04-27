package neo.javax.transaction.xa;

public interface XAResource {
	// Field descriptor #5 I
	  public static final int TMENDRSCAN = 8388608;
	  
	  // Field descriptor #5 I
	  public static final int TMFAIL = 536870912;
	  
	  // Field descriptor #5 I
	  public static final int TMJOIN = 2097152;
	  
	  // Field descriptor #5 I
	  public static final int TMNOFLAGS = 0;
	  
	  // Field descriptor #5 I
	  public static final int TMONEPHASE = 1073741824;
	  
	  // Field descriptor #5 I
	  public static final int TMRESUME = 134217728;
	  
	  // Field descriptor #5 I
	  public static final int TMSTARTRSCAN = 16777216;
	  
	  // Field descriptor #5 I
	  public static final int TMSUCCESS = 67108864;
	  
	  // Field descriptor #5 I
	  public static final int TMSUSPEND = 33554432;
	  
	  // Field descriptor #5 I
	  public static final int XA_RDONLY = 3;
	  
	  // Field descriptor #5 I
	  public static final int XA_OK = 0;
	  
	  // Method descriptor #48 (Ljavax/transaction/xa/Xid;Z)V
	  public abstract void commit(Xid arg0, boolean arg1) throws XAException;
	  
	  // Method descriptor #47 (Ljavax/transaction/xa/Xid;I)V
	  public abstract void end(Xid arg0, int arg1) throws XAException;
	  
	  // Method descriptor #46 (Ljavax/transaction/xa/Xid;)V
	  public abstract void forget(Xid arg0) throws XAException;
	  
	  // Method descriptor #1 ()I
	  public abstract int getTransactionTimeout() throws XAException;
	  
	  // Method descriptor #43 (Ljavax/transaction/xa/XAResource;)Z
	  public abstract boolean isSameRM(XAResource arg0) throws XAException;
	  
	  // Method descriptor #45 (Ljavax/transaction/xa/Xid;)I
	  public abstract int prepare(Xid arg0) throws XAException;
	  
	  // Method descriptor #44 (I)[Ljavax/transaction/xa/Xid;
	  public abstract Xid[] recover(int arg0) throws XAException;
	  
	  // Method descriptor #46 (Ljavax/transaction/xa/Xid;)V
	  public abstract void rollback(Xid arg0) throws XAException;
	  
	  // Method descriptor #2 (I)Z
	  public abstract boolean setTransactionTimeout(int arg0) throws XAException;
	  
	  // Method descriptor #47 (Ljavax/transaction/xa/Xid;I)V
	  public abstract void start(Xid arg0, int arg1) throws XAException;
}
