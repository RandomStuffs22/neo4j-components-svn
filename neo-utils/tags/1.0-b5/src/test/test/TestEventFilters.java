package test;

import java.util.ArrayList;
import java.util.List;
import org.neo4j.impl.event.EventData;
import com.windh.util.neo.CrudEventBufferFilter;
import com.windh.util.neo.CrudEventData;
import com.windh.util.neo.CrudEventFilter;
import com.windh.util.neo.EventContext;
import com.windh.util.neo.CrudEventData.AlterationMode;

public class TestEventFilters extends NeoTest
{
	public void testCrudEventFilter()
	{
		CrudEventFilter filter = new CrudEventFilter();
		assertTrue( filter.pass( null, data( 0, AlterationMode.CREATED ) ) );
		assertTrue( !filter.pass( null, data( 0, AlterationMode.CREATED ) ) );
		assertTrue( !filter.pass( null, data( 0, AlterationMode.MODIFIED ) ) );
		assertTrue( filter.pass( null, data( 0, AlterationMode.DELETED ) ) );
		
		assertTrue( filter.pass( null, data( 1, AlterationMode.MODIFIED ) ) );
		assertTrue( !filter.pass( null, data( 1, AlterationMode.CREATED ) ) );
		assertTrue( !filter.pass( null, data( 1, AlterationMode.MODIFIED ) ) );
		assertTrue( filter.pass( null, data( 1, AlterationMode.DELETED ) ) );
		assertTrue( !filter.pass( null, data( 1, AlterationMode.CREATED ) ) );
		assertTrue( !filter.pass( null, data( 1, AlterationMode.MODIFIED ) ) );
		
		assertTrue( filter.pass( null, data( 2, AlterationMode.DELETED ) ) );
		assertTrue( !filter.pass( null, data( 2, AlterationMode.CREATED ) ) );
		assertTrue( !filter.pass( null, data( 2, AlterationMode.MODIFIED ) ) );
	}
	
	public void testCrudEventBufferFilter()
	{
		List<EventContext> l = new ArrayList<EventContext>();
		l.add( new EventContext( null, data( 0, AlterationMode.CREATED ) ) );
		l.add( new EventContext( null, data( 0, AlterationMode.MODIFIED ) ) );
		l.add( new EventContext( null, data( 0, AlterationMode.MODIFIED ) ) );
		l.add( new EventContext( null, data( 0, AlterationMode.MODIFIED ) ) );
		l.add( new EventContext( null, data( 1, AlterationMode.CREATED ) ) );
		l.add( new EventContext( null, data( 1, AlterationMode.MODIFIED ) ) );
		l.add( new EventContext( null, data( 2, AlterationMode.MODIFIED ) ) );
		l.add( new EventContext( null, data( 2, AlterationMode.MODIFIED ) ) );
		l.add( new EventContext( null, data( 2, AlterationMode.MODIFIED ) ) );

		l.add( new EventContext( null, data( 0, AlterationMode.DELETED ) ) );
		l.add( new EventContext( null, data( 0, AlterationMode.MODIFIED ) ) );
		l.add( new EventContext( null, data( 0, AlterationMode.MODIFIED ) ) );
		
		l.add( new EventContext( null, data( 3, AlterationMode.DELETED ) ) );
		l.add( new EventContext( null, data( 3, AlterationMode.MODIFIED ) ) );
		
		CrudEventBufferFilter filter = new CrudEventBufferFilter();
		EventContext[] a = filter.filter(
			l.toArray( new EventContext[ l.size() ] ) );
		
		assertEquals( 4, a.length );
		assertTrue( isEqual( a[ 0 ], data( 1, AlterationMode.CREATED ) ) );
		assertTrue( isEqual( a[ 1 ], data( 2, AlterationMode.MODIFIED ) ) );
		assertTrue( isEqual( a[ 2 ], data( 0, AlterationMode.DELETED ) ) );
		assertTrue( isEqual( a[ 3 ], data( 3, AlterationMode.DELETED ) ) );
	}
	
	private EventData data( long id, AlterationMode mode )
	{
		return new EventData( new Data( id, mode ) ); 
	}
	
	private boolean isEqual( EventContext context, EventData data )
	{
		CrudEventData d = ( CrudEventData ) data.getData();
		CrudEventData target = ( CrudEventData ) context.getData().getData();
		return target.getNodeId() == d.getNodeId() &&
			target.getAlterationMode() == d.getAlterationMode();
	}
	
	private static class Data extends CrudEventData
	{
		private long id;
		
		Data( long id, AlterationMode mode )
		{
			super( mode );
			this.id = id;
		}
		
		@Override
		public long getNodeId()
		{
			return this.id;
		}
	}
}
