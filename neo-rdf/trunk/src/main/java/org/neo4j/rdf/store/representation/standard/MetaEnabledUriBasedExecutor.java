package org.neo4j.rdf.store.representation.standard;

import org.neo4j.api.core.NeoService;
import org.neo4j.api.core.Node;
import org.neo4j.neometa.structure.MetaStructure;
import org.neo4j.neometa.structure.MetaStructureObject;
import org.neo4j.neometa.structure.MetaStructureThing;
import org.neo4j.rdf.store.representation.AbstractNode;
import org.neo4j.util.index.Index;

/**
 * Adds meta model suport to the {@link UriBasedExecutor}.
 */
public class MetaEnabledUriBasedExecutor extends UriBasedExecutor
{
    /**
     * The RDF namespace base URI.
     */
    public static final String RDF_NAMESPACE =
        "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    
    /**
     * The URI which represents a type of an instance.
     */
    public static final String RDF_TYPE_URI = RDF_NAMESPACE + "type";

    /**
     * The "executor info" key for meta enabled nodes.
     */
    public static final String META_EXECUTOR_INFO_KEY = "meta";

    private final MetaStructure meta;
	
	/**
	 * @param neo the {@link NeoService}.
	 * @param index the {@link Index} to use as an object lookup.
	 * @param meta the {@link MetaStructure} to use as a class/property lookup.
	 */
	public MetaEnabledUriBasedExecutor( NeoService neo, Index index,
		MetaStructure meta )
	{
		super( neo, index );
		this.meta = meta;
	}
	
	private String getMetaExecutorInfo( AbstractNode node )
	{
		return ( String ) node.getExecutorInfo( META_EXECUTOR_INFO_KEY );
	}
	
	private boolean isMeta( AbstractNode node )
	{
		return getMetaExecutorInfo( node ) != null;
	}

	@Override
    protected Node lookupNode( AbstractNode node,
        boolean createIfItDoesntExist )
    {
		Node result = null;
		if ( isMeta( node ) )
		{
			MetaStructureThing thing = getMetaStructureThing( node );
		    result = thing == null ? null : thing.node();
		}
		else
		{
			result = super.lookupNode( node, createIfItDoesntExist );
		}
		return result;
    }
	
	private MetaStructureThing getMetaStructureThing( AbstractNode node )
	{
	    MetaStructureThing thing = null;
	    String metaInfo = getMetaExecutorInfo( node );
	    if ( node.getUriOrNull() == null )
	    {
	    	thing = null;
	    }
	    else if ( metaInfo.equals( "class" ) )
        {
            thing = meta.getGlobalNamespace().getMetaClass(
                node.getUriOrNull().getUriAsString(), false );
        }
        else if ( metaInfo.equals( "property" ) )
        {
            thing = meta.getGlobalNamespace().getMetaProperty(
                node.getUriOrNull().getUriAsString(), false );
        }
        else
        {
            throw new IllegalArgumentException( "Strange meta info '" +
                metaInfo + "'" );
        }
        return thing;
	}

	@Override
	public String getNodeUriPropertyKey( AbstractNode abstractNode )
	{
		return isMeta( abstractNode ) ? MetaStructureObject.KEY_NAME :
		    super.getNodeUriPropertyKey( abstractNode );
	}
	
	@Override
	public Node lookupNode( AbstractNode abstractNode )
	{
	    return lookupNode( abstractNode, false );
	}
}
