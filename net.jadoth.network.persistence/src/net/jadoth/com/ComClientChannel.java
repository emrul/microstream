package net.jadoth.com;

import static net.jadoth.X.notNull;

import net.jadoth.persistence.types.PersistenceManager;

public interface ComClientChannel<C> extends ComChannel
{
	public C connection();
	
	public ComProtocol protocol();
	
	public ComClient<C> parent();
	
	
	
	public static <C> ComClientChannel<C> New(
		final PersistenceManager<?> persistenceManager,
		final C                     connection        ,
		final ComProtocol           protocol          ,
		final ComClient<C>          parent
	)
	{
		return new ComClientChannel.Implementation<>(
			notNull(persistenceManager),
			notNull(connection)        ,
			notNull(protocol)          ,
			notNull(parent)
		);
	}
	
	public final class Implementation<C> extends ComChannel.Implementation implements ComClientChannel<C>
	{
		///////////////////////////////////////////////////////////////////////////
		// instance fields //
		////////////////////
		
		private final C            connection;
		private final ComProtocol  protocol  ;
		private final ComClient<C> parent    ;
		
		
		
		///////////////////////////////////////////////////////////////////////////
		// constructors //
		/////////////////
		
		Implementation(
			final PersistenceManager<?> persistenceManager,
			final C                     connection        ,
			final ComProtocol           protocol          ,
			final ComClient<C>          parent
		)
		{
			super(persistenceManager);
			this.connection = connection;
			this.protocol   = protocol  ;
			this.parent     = parent    ;
		}

		
		
		///////////////////////////////////////////////////////////////////////////
		// methods //
		////////////
		
		@Override
		public final C connection()
		{
			return this.connection;
		}

		@Override
		public final ComProtocol protocol()
		{
			return this.protocol;
		}

		@Override
		public final ComClient<C> parent()
		{
			return this.parent;
		}
		
	}
	
}
