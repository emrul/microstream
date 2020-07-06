package one.microstream.com;

import static one.microstream.X.notNull;

import java.nio.ByteOrder;

import one.microstream.persistence.types.PersistenceIdStrategy;
import one.microstream.persistence.types.PersistenceTypeDictionaryView;
import one.microstream.persistence.types.PersistenceTypeDictionaryViewProvider;
import one.microstream.typing.Immutable;


public interface ComProtocolProvider<C> extends ComProtocolData
{
	public ComProtocol provideProtocol(C connection);
	
	
	
	public static <C> ComProtocolProviderCreator<C> Creator()
	{
		return ComProtocolProviderCreator.New();
	}
	
	public static <C> ComProtocolProvider<C> New(
		final String                                name                  ,
		final String                                version               ,
		final ByteOrder                             byteOrder             ,
		final int                                   inactivityTimeout     ,
		final PersistenceIdStrategy                 idStrategy            ,
		final PersistenceTypeDictionaryViewProvider typeDictionaryProvider,
		final ComProtocolCreator                    protocolCreator
	)
	{
		return new ComProtocolProvider.Default<>(
			notNull(name)                  ,
			notNull(version)               ,
			notNull(byteOrder)             ,
			inactivityTimeout              ,
			notNull(idStrategy)            ,
			notNull(typeDictionaryProvider),
			notNull(protocolCreator)
		);
	}
	
	public final class Default<C> implements ComProtocolProvider<C>, Immutable
	{
		///////////////////////////////////////////////////////////////////////////
		// instance fields //
		////////////////////

		private final String                                name                  ;
		private final String                                version               ;
		private final ByteOrder                             byteOrder             ;
		private final int                                   inactivityTimeout     ;
		private final PersistenceIdStrategy                 idStrategy            ;
		private final PersistenceTypeDictionaryViewProvider typeDictionaryProvider;
		private final ComProtocolCreator                    protocolCreator       ;
		
		
		///////////////////////////////////////////////////////////////////////////
		// constructors //
		/////////////////
		
		Default(
			final String                                name                  ,
			final String                                version               ,
			final ByteOrder                             byteOrder             ,
			final int                                   inactivityTimeout     ,
			final PersistenceIdStrategy                 idStrategy            ,
			final PersistenceTypeDictionaryViewProvider typeDictionaryProvider,
			final ComProtocolCreator                    protocolCreator
		)
		{
			
			super();
			this.name                   = name                  ;
			this.version                = version               ;
			this.byteOrder              = byteOrder             ;
			this.idStrategy             = idStrategy            ;
			this.typeDictionaryProvider = typeDictionaryProvider;
			this.protocolCreator        = protocolCreator       ;
			this.inactivityTimeout      = inactivityTimeout     ;
		}
		
		
		
		///////////////////////////////////////////////////////////////////////////
		// methods //
		////////////

		@Override
		public final String name()
		{
			return this.name;
		}

		@Override
		public final String version()
		{
			return this.version;
		}

		@Override
		public final ByteOrder byteOrder()
		{
			return this.byteOrder;
		}

		@Override
		public final PersistenceIdStrategy idStrategy()
		{
			return this.idStrategy;
		}
		
		@Override
		public final PersistenceTypeDictionaryView typeDictionary()
		{
			return this.typeDictionaryProvider.provideTypeDictionary();
		}
		
		@Override
		public int inactivityTimeout()
		{
			return this.inactivityTimeout;
		}
		
		@Override
		public ComProtocol provideProtocol(final C connection)
		{
			// the default implementation assigns the same id range to every client, hence no reference to connection
			return this.protocolCreator.creatProtocol(
				this.name()             ,
				this.version()          ,
				this.byteOrder()        ,
				this.inactivityTimeout(),
				this.idStrategy()       ,
				this.typeDictionary()
			);
		}
		
	}
		
}
