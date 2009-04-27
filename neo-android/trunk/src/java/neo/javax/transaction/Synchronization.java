package neo.javax.transaction;

public interface Synchronization {

	void beforeCompletion();

	void afterCompletion(int status);

}
