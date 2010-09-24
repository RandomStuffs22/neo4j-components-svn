package org.neo4j.graphalgo.impl.path;

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
import org.neo4j.graphalgo.impl.util.PathImpl;
import org.neo4j.graphalgo.impl.util.PathImpl.Builder;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipExpander;
import org.neo4j.helpers.collection.IterableWrapper;
import org.neo4j.helpers.collection.NestingIterator;
import org.neo4j.helpers.collection.PrefetchingIterator;

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
public class ShortestPath implements PathFinder<Path>
{
    private final int maxDepth;
    private final RelationshipExpander relExpander;
    private final HitDecider hitDecider;
    
    /**
     * Constructs a new stortest path algorithm.
     * @param maxDepth the maximum depth for the traversal. Returned paths
     * will never have a greater {@link Path#length()} than {@code maxDepth}.
     * @param relExpander the {@link RelationshipExpander} to use for deciding
     * which relationships to expand for each {@link Node}.
     */
    public ShortestPath( int maxDepth, RelationshipExpander relExpander )
    {
        this( maxDepth, relExpander, false );
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
    public ShortestPath( int maxDepth, RelationshipExpander relExpander, boolean findPathsOnMaxDepthOnly )
    {
        this.maxDepth = maxDepth;
        this.relExpander = relExpander;
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
                sharedCurrentDepth, stopAsap, relExpander );
        final DirectionData endData = new DirectionData( end,
                sharedVisitedRels, sharedFrozenDepth, sharedStop,
                sharedCurrentDepth, stopAsap, relExpander.reversed() );
        
        while ( startData.hasNext() || endData.hasNext() )
        {
            goOneStep( startData, endData, hits, stopAsap, startData );
            goOneStep( endData, startData, hits, stopAsap, startData );
        }
        
        Collection<Hit> least = hits.least();
        return least != null ? hitsToPaths( least, start, end ) : Collections.<Path>emptyList();
    }

    // Few long-lived instances
    private static class Hit
    {
        private final DirectionData start;
        private final DirectionData end;
        private final Node connectingNode;
        
        Hit( DirectionData start, DirectionData end, Node connectingNode )
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
        
        Node nextNode = directionData.next();
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
    protected class DirectionData extends PrefetchingIterator<Node>
    {
        private final Node startNode;
        private int currentDepth;
        private Iterator<Relationship> nextRelationships;
        private final Collection<Node> nextNodes = new ArrayList<Node>();
        private Map<Node, LevelData> visitedNodes = new HashMap<Node, LevelData>();
        private Node lastParentTraverserNode;
        private final MutableInteger sharedFrozenDepth;
        private final MutableBoolean sharedStop;
        private final MutableInteger sharedCurrentDepth;
        private boolean haveFoundSomething;
        private boolean stop;
        private final boolean stopAsap;
        private final RelationshipExpander expander;
        
        DirectionData( Node startNode, Collection<Long> sharedVisitedRels,
                MutableInteger sharedFrozenDepth, MutableBoolean sharedStop,
                MutableInteger sharedCurrentDepth, boolean stopAsap,
                RelationshipExpander expander )
        {
            this.startNode = startNode;
            this.visitedNodes.put( startNode, new LevelData( null, 0 ) );
            this.nextNodes.add( startNode );
            this.sharedFrozenDepth = sharedFrozenDepth;
            this.sharedStop = sharedStop;
            this.sharedCurrentDepth = sharedCurrentDepth;
            this.stopAsap = stopAsap;
            this.expander = expander;
            prepareNextLevel();
        }
        
        private void prepareNextLevel()
        {
            Collection<Node> nodesToIterate = new ArrayList<Node>(
                    filterNextLevelNodes( this.nextNodes ) );
            this.nextNodes.clear();
            this.nextRelationships = new NestingIterator<Relationship, Node>(
                    nodesToIterate.iterator() )
            {
                @Override
                protected Iterator<Relationship> createNestedIterator( Node node )
                {
                    lastParentTraverserNode = node;
                    return expander.expand( node ).iterator();
                }
            };
            this.currentDepth++;
            this.sharedCurrentDepth.value = this.sharedCurrentDepth.value + 1;
        }
        
        @Override
        protected Node fetchNextOrNull()
        {
            while ( true )
            {
                Relationship nextRel = fetchNextRelOrNull();
                if ( nextRel == null )
                {
                    return null;
                }
                
                Node result = nextRel.getOtherNode( this.lastParentTraverserNode );
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
        
        private Relationship fetchNextRelOrNull()
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
    
    protected Collection<Node> filterNextLevelNodes( Collection<Node> nextNodes )
    {
        return nextNodes;
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
        
        LevelData( Relationship relToHere, int depth )
        {
            if ( relToHere != null )
            {
                addRel( relToHere );
            }
            this.depth = depth;
        }
        
        void addRel( Relationship rel )
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
        private final LinkedList<Relationship> rels;
        private final Node node;
        
        PathData( Node node, LinkedList<Relationship> rels )
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
            Iterable<LinkedList<Relationship>> startPaths = getPaths( hit, hit.start );
            Iterable<LinkedList<Relationship>> endPaths = getPaths( hit, hit.end );
            for ( LinkedList<Relationship> startPath : startPaths )
            {
                PathImpl.Builder startBuilder = toBuilder( start, startPath );
                for ( LinkedList<Relationship> endPath : endPaths )
                {
                    PathImpl.Builder endBuilder = toBuilder( end, endPath );
                    Path path = startBuilder.build( endBuilder );
                    paths.add( path );
                }
            }
        }
        return paths;
    }
    
    private static Iterable<LinkedList<Relationship>> getPaths( Hit hit, DirectionData data )
    {
        LevelData levelData = data.visitedNodes.get( hit.connectingNode );
        if ( levelData.depth == 0 )
        {
            Collection<LinkedList<Relationship>> result = new ArrayList<LinkedList<Relationship>>();
            result.add( new LinkedList<Relationship>() );
            return result;
        }
        
        Collection<PathData> set = new ArrayList<PathData>();
        GraphDatabaseService graphDb = data.startNode.getGraphDatabase();
        for ( long rel : levelData.relsToHere )
        {
            set.add( new PathData( hit.connectingNode, new LinkedList<Relationship>(
                    Arrays.asList( graphDb.getRelationshipById( rel ) ) ) ) );
        }
        for ( int i = 0; i < levelData.depth - 1; i++ )
        {
            // One level
            Collection<PathData> nextSet = new ArrayList<PathData>();
            for ( PathData entry : set )
            {
                // One path...
                int counter = 0;
                Node otherNode = entry.rels.getFirst().getOtherNode( entry.node );
                LevelData otherLevelData = data.visitedNodes.get( otherNode );
                for ( long rel : otherLevelData.relsToHere )
                {
                    // ...may split into several paths
                    LinkedList<Relationship> rels = counter++ == 0 ? entry.rels : 
                        new LinkedList<Relationship>( entry.rels );
                    rels.addFirst( graphDb.getRelationshipById( rel ) );
                    nextSet.add( new PathData( otherNode, rels ) );
                }
            }
            set = nextSet;
        }
        
        return new IterableWrapper<LinkedList<Relationship>, PathData>( set )
        {
            @Override
            protected LinkedList<Relationship> underlyingObjectToObject( PathData object )
            {
                return object.rels;
            }
        };
    }

    private static Builder toBuilder( Node startNode, LinkedList<Relationship> rels )
    {
        PathImpl.Builder builder = new PathImpl.Builder( startNode );
        for ( Relationship rel : rels )
        {
            builder = builder.push( rel );
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
}
