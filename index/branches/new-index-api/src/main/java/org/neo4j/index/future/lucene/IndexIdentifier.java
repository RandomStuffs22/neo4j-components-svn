package org.neo4j.index.future.lucene;

import java.util.Map;

class IndexIdentifier
{
    final Class<?> itemsClass;
    final String indexName;
    private IndexType type;
    
    public IndexIdentifier( Class<?> itemsClass, String indexName )
    {
        this.itemsClass = itemsClass;
        this.indexName = indexName;
    }
    
    public IndexIdentifier( Class<?> itemsClass, String indexName,
            IndexType type )
    {
        this( itemsClass, indexName );
        this.type = type;
    }
    
    IndexType getType( Map<Object, Object> config )
    {
        if ( type == null )
        {
            // Two or more threads might do this at the same time, but it
            // is OK
            type = IndexType.getIndexType( config, indexName );
        }
        return type;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( o == null || !getClass().equals( o.getClass() ) )
        {
            return false;
        }
        IndexIdentifier i = (IndexIdentifier) o;
        return itemsClass.equals( i.itemsClass ) &&
                indexName.equals( i.indexName );
    }
    
    @Override
    public int hashCode()
    {
        int code = 17;
        code += 7*itemsClass.hashCode();
        code += 7*indexName.hashCode();
        return code;
    }
}