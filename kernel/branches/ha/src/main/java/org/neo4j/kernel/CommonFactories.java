package org.neo4j.kernel;

import java.util.HashMap;
import java.util.Map;

import javax.transaction.TransactionManager;

import org.neo4j.kernel.impl.core.DefaultRelationshipTypeCreator;
import org.neo4j.kernel.impl.core.RelationshipTypeCreator;
import org.neo4j.kernel.impl.nioneo.store.IdGenerator;
import org.neo4j.kernel.impl.nioneo.store.IdGeneratorImpl;
import org.neo4j.kernel.impl.transaction.LockManager;
import org.neo4j.kernel.impl.transaction.TxModule;
import org.neo4j.kernel.impl.transaction.TxRollbackHook;
import org.neo4j.kernel.impl.transaction.xaframework.TxIdGenerator;
import org.neo4j.kernel.impl.transaction.xaframework.TxIdGeneratorFactory;

public class CommonFactories
{
    public static LockManagerFactory defaultLockManagerFactory()
    {
        return new LockManagerFactory()
        {
            public LockManager create( TxModule txModule )
            {
                return new LockManager( txModule.getTxManager() );
            }
        };
    }
    
    public static IdGeneratorFactory defaultIdGeneratorFactory()
    {
        return new IdGeneratorFactory()
        {
            private final Map<IdType, IdGenerator> generators = new HashMap<IdType, IdGenerator>();
            
            public IdGenerator open( String fileName, int grabSize, IdType idType,
                    long highestIdInUse )
            {
                IdGenerator generator = new IdGeneratorImpl( fileName, grabSize );
                generators.put( idType, generator );
                return generator;
            }
            
            public IdGenerator get( IdType idType )
            {
                return generators.get( idType );
            }
            
            public void create( String fileName )
            {
                IdGeneratorImpl.createGenerator( fileName );
            }
        };
    }
    
    public static RelationshipTypeCreator defaultRelationshipTypeCreator()
    {
        return new DefaultRelationshipTypeCreator();
    }
    
    public static TxIdGeneratorFactory defaultTxIdGeneratorFactory()
    {
        return new TxIdGeneratorFactory()
        {
            public TxIdGenerator create( final TransactionManager txManager )
            {
                return TxIdGenerator.DEFAULT;
            }
        }; 
    }
    
    public static TxRollbackHook defaultTxRollbackHook()
    {
        return new TxRollbackHook()
        {
            public void rollbackTransaction( int eventIdentifier )
            {
                // Do nothing from the ordinary here
            }
        };
    }
}