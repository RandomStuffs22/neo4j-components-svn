package org.neo4j.neometa.structure;

import java.util.Collection;

import org.neo4j.api.core.Node;

/**
 * The access point of a meta model. Is given a root node where all the
 * namespaces, properties and classes are stored/read underneath.
 */
public interface MetaStructure
{
	/**
	 * Returns (and optionally creates) a {@link MetaStructureNamespace}
	 * instance (with underlying {@link Node}).
	 * @param name the name for the namespace.
	 * @param allowCreate if {@code true} and no namespace by the given
	 * {@code name} exists then it is created.
	 * @return the {@link MetaStructureNamespace} in this namespace with the
	 * given {@code name}.
	 */
	MetaStructureNamespace getNamespace( String name, boolean allowCreate );

	/**
	 * @return the global namespace (without a name) which always exists.
	 * It's actually created on demand the first time. A call to
	 * {@link MetaStructureNamespace#getName()} will fail for this namespace.
	 */
	MetaStructureNamespace getGlobalNamespace();

	/**
	 * @return a modifiable collection of all {@link MetaStructureNamespace}
	 * instances for this meta model.
	 */
	Collection<MetaStructureNamespace> getNamespaces();

	/**
	 * Looks up a value from the meta model, considering restrictions and
	 * hierarchy.
	 * @param <T> the type of the returned value.
	 * @param property the property to get a value from (also considering
	 * restrictions).
	 * @param finder the value finder for a specific value, f.ex.
	 * minimum cardinality.
	 * @param classes the classes to look in.
	 * @return the found value or {@code null} if no value was found.
	 */
	<T> T lookup( MetaStructureProperty property, LookerUpper<T> finder,
		MetaStructureClass... classes );

	/**
	 * Looks up the min cardinality property.
	 */
	public static LookerUpper<Integer> LOOKUP_MIN_CARDINALITY =
		new LookerUpper<Integer>()
	{
		public Integer get( MetaStructureRestrictable restrictable )
		{
			return restrictable.getMinCardinality();
		}
	};

	/**
	 * Looks up the max cardinality property.
	 */
	public static LookerUpper<Integer> LOOKUP_MAX_CARDINALITY =
		new LookerUpper<Integer>()
	{
		public Integer get( MetaStructureRestrictable restrictable )
		{
			return restrictable.getMaxCardinality();
		}
	};

	/**
	 * Looks up the property range property.
	 */
	public static LookerUpper<PropertyRange> LOOKUP_PROPERTY_RANGE =
		new LookerUpper<PropertyRange>()
	{
		public PropertyRange get( MetaStructureRestrictable restrictable )
		{
			return restrictable.getRange();
		}
	};
}
