package neo.javax.transaction;

public interface Status {

	  
	  // Field descriptor #14 I
	  public static final int STATUS_ACTIVE = 0;
	  
	  // Field descriptor #14 I
	  public static final int STATUS_MARKED_ROLLBACK = 1;
	  
	  // Field descriptor #14 I
	  public static final int STATUS_PREPARED = 2;
	  
	  // Field descriptor #14 I
	  public static final int STATUS_COMMITTED = 3;
	  
	  // Field descriptor #14 I
	  public static final int STATUS_ROLLEDBACK = 4;
	  
	  // Field descriptor #14 I
	  public static final int STATUS_UNKNOWN = 5;
	  
	  // Field descriptor #14 I
	  public static final int STATUS_NO_TRANSACTION = 6;
	  
	  // Field descriptor #14 I
	  public static final int STATUS_PREPARING = 7;
	  
	  // Field descriptor #14 I
	  public static final int STATUS_COMMITTING = 8;
	  
	  // Field descriptor #14 I
	  public static final int STATUS_ROLLING_BACK = 9;

}
