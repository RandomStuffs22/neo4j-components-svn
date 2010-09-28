package org.neo4j.graphalgo.impl.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphalgo.impl.util.PathImpl.Builder;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.RelationshipExpander;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.helpers.Pair;
import org.neo4j.helpers.Predicate;
import org.neo4j.helpers.collection.FilteringIterator;
import org.neo4j.helpers.collection.IterableWrapper;
import org.neo4j.helpers.collection.NestingIterator;
import org.neo4j.helpers.collection.PrefetchingIterator;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.kernel.impl.core.NodeImpl;
import org.neo4j.kernel.impl.core.NodeManager;
import org.neo4j.kernel.impl.core.RelationshipImpl;

/**
 * Find (all or one) simple shortest path(s) between two nodes. It starts
 * from both ends and goes one relationship at the time, alternating side
 * between each traversal. It does so to minimize the traversal overhead
 * if one side has a very large amount of relationships, but the other one
 * very few. It performs well however the graph is proportioned.
 * 
 * Relationships are traversed in the specified directions from the start node,
 * but in the reverse direction ( {@link Direction#reverse()} ) from the
 * end node. This doesn't affect {@link Direction#BOTH}.
 */
public class ShortestPathOpti implements PathFinder<Path>
{
    private final int maxDepth;
    private final HitDecider hitDecider;
    private final Collection<Pair<RelationshipType, Direction>> types;
    
    /**
     * Constructs a new stortest path algorithm.
     * @param maxDepth the maximum depth for the traversal. Returned paths
     * will never have a greater {@link Path#length()} than {@code maxDepth}.
     * @param relExpander the {@link RelationshipExpander} to use for deciding
     * which relationships to expand for each {@link Node}.
     */
    public ShortestPathOpti( int maxDepth, Collection<Pair<RelationshipType, Direction>> types )
    {
        this( maxDepth, types, false );
    }
    
    /**
     * Constructs a new stortest path algorithm.
     * @param maxDepth the maximum depth for the traversal. Returned paths
     * will never have a greater {@link Path#length()} than {@code maxDepth}.
     * @param relExpander the {@link RelationshipExpander} to use for deciding
     * which relationships to expand for each {@link Node}.
     * @param findPathsOnMaxDepthOnly if {@code true} then it will only try to
     * find paths on that particular depth ({@code maxDepth}).
     */
    public ShortestPathOpti( int maxDepth, Collection<Pair<RelationshipType, Direction>> types,
            boolean findPathsOnMaxDepthOnly )
    {
        this.maxDepth = maxDepth;
        this.types = types;
        this.hitDecider = findPathsOnMaxDepthOnly ? new DepthHitDecider( maxDepth ) : YES_HIT_DECIDER;
    }
    
    public Iterable<Path> findAllPaths( Node start, Node end )
    {
        return internalPaths( start, end, false );
    }
    
    public Path findSinglePath( Node start, Node end )
    {
        Iterator<Path> paths = internalPaths( start, end, true ).iterator();
        return paths.hasNext() ? paths.next() : null;
    }
    
    private Iterable<Path> internalPaths( Node start, Node end, boolean stopAsap )
    {
        if ( start.equals( end ) )
        {
            return Arrays.asList( PathImpl.singular( start ) );
        }

        Hits hits = new Hits();
        Collection<Long> sharedVisitedRels = new HashSet<Long>();
        MutableInteger sharedFrozenDepth = new MutableInteger( MutableInteger.NULL );
        MutableBoolean sharedStop = new MutableBoolean();
        MutableInteger sharedCurrentDepth = new MutableInteger( 0 );
        final DirectionData startData = new DirectionData( start,
                sharedVisitedRels, sharedFrozenDepth, sharedStop,
                sharedCurrentDepth, stopAsap, types );
        final DirectionData endData = new DirectionData( end,
                sharedVisitedRels, sharedFrozenDepth, sharedStop,
                sharedCurrentDepth, stopAsap, reverse( types ) );
        
        while ( startData.hasNext() || endData.hasNext() )
        {
            goOneStep( startData, endData, hits, stopAsap, startData );
            goOneStep( endData, startData, hits, stopAsap, startData );
        }
        
        Collection<Hit> least = hits.least();
        return least != null ? hitsToPaths( least, start, end ) : Collections.<Path>emptyList();
    }

    private Collection<Pair<RelationshipType, Direction>> reverse( Collection<Pair<RelationshipType, Direction>> types )
    {
        Collection<Pair<RelationshipType, Direction>> result = new ArrayList<Pair<RelationshipType,Direction>>();
        for ( Pair<RelationshipType, Direction> entry : types )
        {
            result.add( new Pair<RelationshipType, Direction>( entry.first(), entry.other().reverse() ) );
        }
        return result;
    }

    // Few long-lived instances
    private static class Hit
    {
        private final DirectionData start;
        private final DirectionData end;
        private final NodeImpl connectingNode;
        
        Hit( DirectionData start, DirectionData end, NodeImpl connectingNode )
        {
            this.start = start;
            this.end = end;
            this.connectingNode = connectingNode;
        }
        
        @Override
        public int hashCode()
        {
            return connectingNode.hashCode();
        }
        
        @Override
        public boolean equals( Object obj )
        {
            Hit o = (Hit) obj;
            return connectingNode.equals( o.connectingNode );
        }
    }

    private void goOneStep( DirectionData directionData, DirectionData otherSide, Hits hits,
            boolean stopAsEarlyAsPossible, DirectionData startSide )
    {
        if ( !directionData.hasNext() )
        {
            return;
        }
        
        NodeImpl nextNode = directionData.next();
        LevelData otherSideHit = otherSide.visitedNodes.get( nextNode );
        if ( otherSideHit != null )
        {
            // This is a hit
            int depth = directionData.currentDepth + otherSideHit.depth;
            if ( !hitDecider.isHit( depth ) )
            {
                return;
            }
            
            if ( directionData.sharedFrozenDepth.value == MutableInteger.NULL )
            {
                directionData.sharedFrozenDepth.value = depth;
            }
            if ( depth <= directionData.sharedFrozenDepth.value )
            {
                directionData.haveFoundSomething = true;
                if ( depth < directionData.sharedFrozenDepth.value )
                {
                    directionData.sharedFrozenDepth.value = depth;
                    // TODO Is it really ok to just stop the other side here?
                    // I'm basing that decision on that it was the other side
                    // which found the deeper paths (correct assumption?)
                    otherSide.stop = true;
                    if ( stopAsEarlyAsPossible )
                    {
                        // we can stop here because we won't get a less deep path than this.
                        directionData.sharedStop.value = true;
                    }
                }
                
                // Add it to the list of hits
                DirectionData startSideData =
                        directionData == startSide ? directionData : otherSide;
                DirectionData endSideData =
                        directionData == startSide ? otherSide : directionData;
                hits.add( new Hit( startSideData, endSideData, nextNode ), depth );
            }
        }
    }
    
    // Two long-lived instances
    protected class DirectionData extends PrefetchingIterator<NodeImpl>
    {
        private final GraphDatabaseService graphDb;
        private final NodeImpl startNode;
        private int currentDepth;
        private Iterator<RelationshipImpl> nextRelationships;
        private final Collection<NodeImpl> nextNodes = new ArrayList<NodeImpl>();
        private Map<NodeImpl, LevelData> visitedNodes = new HashMap<NodeImpl, LevelData>();
        private NodeImpl lastParentTraverserNode;
        private final MutableInteger sharedFrozenDepth;
        private final MutableBoolean sharedStop;
        private final MutableInteger sharedCurrentDepth;
        private boolean haveFoundSomething;
        private boolean stop;
        private final boolean stopAsap;
        private final NodeManager nm;
        private final Expander expander;
        
        DirectionData( Node startNode, Collection<Long> sharedVisitedRels,
                MutableInteger sharedFrozenDepth, MutableBoolean sharedStop,
                MutableInteger sharedCurrentDepth, boolean stopAsap,
                Collection<Pair<RelationshipType, Direction>> types )
        {
            this.graphDb = startNode.getGraphDatabase();
            this.nm = ((EmbeddedGraphDatabase) startNode.getGraphDatabase()).getConfig().getGraphDbModule().getNodeManager();
            this.startNode = nm.getNodeForProxy( (int) startNode.getId() );
            this.visitedNodes.put( this.startNode, new LevelData( null, 0 ) );
            this.nextNodes.add( this.startNode );
            this.sharedFrozenDepth = sharedFrozenDepth;
            this.sharedStop = sharedStop;
            this.sharedCurrentDepth = sharedCurrentDepth;
            this.stopAsap = stopAsap;
            this.expander = new Expander( nm, types );
            prepareNextLevel();
        }
        
        private void prepareNextLevel()
        {
            Collection<NodeImpl> nodesToIterate = new ArrayList<NodeImpl>( this.nextNodes );
            this.nextNodes.clear();
            this.nextRelationships = new NestingIterator<RelationshipImpl, NodeImpl>(
                    nodesToIterate.iterator() )
            {
                @Override
                protected Iterator<RelationshipImpl> createNestedIterator( NodeImpl node )
                {
                    lastParentTraverserNode = node;
                    return expander.doExpand( node );
                }
            };
            this.currentDepth++;
            this.sharedCurrentDepth.value = this.sharedCurrentDepth.value + 1;
        }

        @Override
        protected NodeImpl fetchNextOrNull()
        {
            while ( true )
            {
                RelationshipImpl nextRel = fetchNextRelOrNull();
                if ( nextRel == null )
                {
                    return null;
                }
                
                NodeImpl result = nextRel.getOtherNodeImpl( nm, this.lastParentTraverserNode );
                LevelData levelData = this.visitedNodes.get( result );
                boolean createdLevelData = false;
                if ( levelData == null )
                {
                    levelData = new LevelData( nextRel, this.currentDepth );
                    this.visitedNodes.put( result, levelData );
                    createdLevelData = true;
                }
                
                if ( this.currentDepth < levelData.depth )
                {
                    throw new RuntimeException( "This shouldn't happen... I think" );
                }
                else if ( !this.stopAsap && this.currentDepth == levelData.depth &&
                        !createdLevelData )
                {
                    levelData.addRel( nextRel );
                }
                
                // Have we visited this node before? In that case don't add it
                // as next node to traverse
                if ( !createdLevelData )
                {
                    continue;
                }
                
                this.nextNodes.add( result );
                return result;
            }
        }
        
        private boolean canGoDeeper()
        {
            return this.sharedFrozenDepth.value == MutableInteger.NULL &&
                    this.sharedCurrentDepth.value < maxDepth;
        }
        
        private RelationshipImpl fetchNextRelOrNull()
        {
            boolean stopped = this.stop || this.sharedStop.value;
            boolean hasComeTooFarEmptyHanded = this.sharedFrozenDepth.value != MutableInteger.NULL
                    && this.sharedCurrentDepth.value > this.sharedFrozenDepth.value
                    && !this.haveFoundSomething;
            if ( stopped || hasComeTooFarEmptyHanded )
            {
                return null;
            }
            
            if ( !this.nextRelationships.hasNext() )
            {
                if ( canGoDeeper() )
                {
                    prepareNextLevel();
                }
            }
            return this.nextRelationships.hasNext() ? this.nextRelationships.next() : null;
        }
    }
    
    // Few long-lived instances
    private static class MutableInteger
    {
        private static final int NULL = -1;
        
        private int value;
        
        MutableInteger( int initialValue )
        {
            this.value = initialValue;
        }
    }
    
    // Few long-lived instances
    private static class MutableBoolean
    {
        private boolean value;
    }
    
    // Many long-lived instances
    private static class LevelData
    {
        private long[] relsToHere;
        private int depth;
        
        LevelData( RelationshipImpl relToHere, int depth )
        {
            if ( relToHere != null )
            {
                addRel( relToHere );
            }
            this.depth = depth;
        }
        
        void addRel( RelationshipImpl rel )
        {
            long[] newRels = null;
            if ( relsToHere == null )
            {
                newRels = new long[1];
            }
            else
            {
                newRels = new long[relsToHere.length+1];
                System.arraycopy( relsToHere, 0, newRels, 0, relsToHere.length );
            }
            newRels[newRels.length-1] = rel.getId();
            relsToHere = newRels;
        }
    }
    
    // One long lived instance
    private static class Hits
    {
        private Map<Integer, Collection<Hit>> hits =
            new HashMap<Integer, Collection<Hit>>();
        private int lowestDepth;
        
        void add( Hit hit, int atDepth )
        {
            Collection<Hit> depthHits = hits.get( atDepth );
            if ( depthHits == null )
            {
                depthHits = new HashSet<Hit>();
                hits.put( atDepth, depthHits );
            }
            depthHits.add( hit );
            if ( lowestDepth == 0 || atDepth < lowestDepth )
            {
                lowestDepth = atDepth;
            }
        }
        
        Collection<Hit> least()
        {
            return hits.get( lowestDepth );
        }
    }

    // Methods for converting data representing paths to actual Path instances.
    // It's rather tricky just because this algo stores as little info as possible
    // required to build paths from hit information.
    
    private static class PathData
    {
        private final LinkedList<RelationshipImpl> rels;
        private final NodeImpl node;
        
        PathData( NodeImpl node, LinkedList<RelationshipImpl> rels )
        {
            this.rels = rels;
            this.node = node;
        }
    }
    
    private static Iterable<Path> hitsToPaths( Collection<Hit> depthHits, Node start, Node end )
    {
        Collection<Path> paths = new ArrayList<Path>();
        for ( Hit hit : depthHits )
        {
            Iterable<LinkedList<RelationshipImpl>> startPaths = getPaths( hit, hit.start );
            Iterable<LinkedList<RelationshipImpl>> endPaths = getPaths( hit, hit.end );
            for ( LinkedList<RelationshipImpl> startPath : startPaths )
            {
                PathImpl.Builder startBuilder = toBuilder( start, startPath );
                for ( LinkedList<RelationshipImpl> endPath : endPaths )
                {
                    PathImpl.Builder endBuilder = toBuilder( end, endPath );
                    Path path = startBuilder.build( endBuilder );
                    paths.add( path );
                }
            }
        }
        return paths;
    }
    
    private static Iterable<LinkedList<RelationshipImpl>> getPaths( Hit hit, DirectionData data )
    {
        LevelData levelData = data.visitedNodes.get( hit.connectingNode );
        if ( levelData.depth == 0 )
        {
            Collection<LinkedList<RelationshipImpl>> result = new ArrayList<LinkedList<RelationshipImpl>>();
            result.add( new LinkedList<RelationshipImpl>() );
            return result;
        }
        
        Collection<PathData> set = new ArrayList<PathData>();
        GraphDatabaseService graphDb = data.graphDb;
        for ( long rel : levelData.relsToHere )
        {
            set.add( new PathData( hit.connectingNode,
                    new LinkedList<RelationshipImpl>( Arrays.asList( data.nm.getRelForProxy( (int) rel ) ) ) ) );
        }
        for ( int i = 0; i < levelData.depth - 1; i++ )
        {
            // One level
            Collection<PathData> nextSet = new ArrayList<PathData>();
            for ( PathData entry : set )
            {
                // One path...
                int counter = 0;
                NodeImpl otherNode = entry.rels.getFirst().getOtherNodeImpl( data.nm, entry.node );
                LevelData otherLevelData = data.visitedNodes.get( otherNode );
                for ( long rel : otherLevelData.relsToHere )
                {
                    // ...may split into several paths
                    LinkedList<RelationshipImpl> rels = counter++ == 0 ? entry.rels : 
                        new LinkedList<RelationshipImpl>( entry.rels );
                    rels.addFirst( data.nm.getRelForProxy( (int) rel ) );
                    nextSet.add( new PathData( otherNode, rels ) );
                }
            }
            set = nextSet;
        }
        
        return new IterableWrapper<LinkedList<RelationshipImpl>, PathData>( set )
        {
            @Override
            protected LinkedList<RelationshipImpl> underlyingObjectToObject( PathData object )
            {
                return object.rels;
            }
        };
    }

    private static Builder toBuilder( Node startNode, LinkedList<RelationshipImpl> rels )
    {
        PathImpl.Builder builder = new PathImpl.Builder( startNode );
        for ( RelationshipImpl rel : rels )
        {
            builder = builder.push( startNode.getGraphDatabase().getRelationshipById( rel.getId() ) );
        }
        return builder;
    }
    
    private static interface HitDecider
    {
        boolean isHit( int depth );
    }
    
    private static final HitDecider YES_HIT_DECIDER = new HitDecider()
    {
        public boolean isHit( int depth )
        {
            return true;
        }
    };
    
    private static class DepthHitDecider implements HitDecider
    {
        private final int depth;

        DepthHitDecider( int depth )
        {
            this.depth = depth;
        }
        
        public boolean isHit( int depth )
        {
            return this.depth == depth;
        }
    }
    
    private static class Expander
    {
        private final Map<String, Direction> typeMap;
        private final RelationshipType[] types;
        private final boolean hasDirections;
        private final NodeManager nm;
        
        Expander( NodeManager nm, Collection<Pair<RelationshipType, Direction>> types )
        {
            this.nm = nm;
            Map<String, Direction> typeMap = new HashMap<String, Direction>();
            this.types = new RelationshipType[types.size()];
            int counter = 0;
            boolean hasDirs = false;
            for ( Pair<RelationshipType, Direction> entry : types )
            {
                typeMap.put( entry.first().name(), entry.other() );
                this.types[counter++] = entry.first();
                if ( !hasDirs && entry.other() != Direction.BOTH )
                {
                    hasDirs = true;
                }
            }
            this.hasDirections = hasDirs;
            this.typeMap = typeMap;
        }
        
        private Iterator<RelationshipImpl> doExpand( final NodeImpl start )
        {
            Iterable<RelationshipImpl> relationships = start.getRelationshipsImpl( nm, types );
            if ( !hasDirections )
            {
                return relationships.iterator();
            }
            else
            {
                return new FilteringIterator<RelationshipImpl>(
                        relationships.iterator(), new Predicate<RelationshipImpl>()
                        {
                            public boolean accept( RelationshipImpl rel )
                            {
                                Direction dir = typeMap.get( rel.getType().name() );
                                return matchDirection( dir == null ? Direction.BOTH : dir, start, rel );
                            }
                        } );
            }
        }
        
        boolean matchDirection( Direction dir, NodeImpl start, RelationshipImpl rel )
        {
            switch ( dir )
            {
            case INCOMING:
                return rel.getEndNodeId() == start.getId();
            case OUTGOING:
                return rel.getStartNodeId() == start.getId();
            }
            return true;
        }
    }
}
