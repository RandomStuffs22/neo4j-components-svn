package org.neo4j.util;

import org.neo4j.graphdb.GraphDatabaseService;

/**
 * A migrator for migration of one version.
 */
public interface Migrator
{
	/**
	 * Performs the migration for this version.
	 * @param graphDb the {@link GraphDatabaseService} used.
	 * @throws RuntimeException if the migration should fail, which it shouldn't
	 */
	public void performMigration( GraphDatabaseService graphDb );
}
