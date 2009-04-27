package neo.javax.transaction.xa;

public interface Xid {
	  
	  // Field descriptor #4 I
	  public static final int MAXGTRIDSIZE = 64;
	  
	  // Field descriptor #4 I
	  public static final int MAXBQUALSIZE = 64;
	  
	  // Method descriptor #1 ()I
	  public abstract int getFormatId();
	  
	  // Method descriptor #2 ()[B
	  public abstract byte[] getGlobalTransactionId();
	  
	  // Method descriptor #2 ()[B
	  public abstract byte[] getBranchQualifier();
	
}
