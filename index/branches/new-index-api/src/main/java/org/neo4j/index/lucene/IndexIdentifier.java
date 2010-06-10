package org.neo4j.index.lucene;

import java.util.Map;

class IndexIdentifier
{
    final Class<?> itemsClass;
    final String indexName;
    final Map<String, String> customConfig;
    private IndexType type;
    
    public IndexIdentifier( Class<?> itemsClass, String indexName,
            Map<String, String> customConfig )
    {
        this.itemsClass = itemsClass;
        this.indexName = indexName;
        this.customConfig = customConfig;
    }
    
    IndexType getType( IndexConfig storedConfig )
    {
        if ( type == null )
        {
            // Two or more threads might do this at the same time, but it
            // is OK
            type = IndexType.getIndexType( storedConfig, customConfig, indexName );
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
