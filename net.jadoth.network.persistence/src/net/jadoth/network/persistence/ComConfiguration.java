package net.jadoth.network.persistence;

import static net.jadoth.X.notNull;

import java.nio.ByteOrder;

import net.jadoth.chars.VarString;
import net.jadoth.persistence.types.PersistenceTypeDictionaryView;
import net.jadoth.swizzling.types.SwizzleIdStrategy;
import net.jadoth.typing.Immutable;

public interface ComConfiguration
{
	public PersistenceTypeDictionaryView typeDictionary();
	
	public ByteOrder byteOrder();
	
	public String version();
	
	public String protocolName();
	
	public SwizzleIdStrategy idStrategy();
	
	
	
	public static ComConfiguration New(
		final PersistenceTypeDictionaryView typeDictionary,
		final ByteOrder                     byteOrder     ,
		final String                        version       ,
		final String                        protocolName  ,
		final SwizzleIdStrategy             idStrategy
	)
	{
		return new ComConfiguration.Implementation(
			notNull(typeDictionary),
			notNull(byteOrder)     ,
			notNull(version)       ,
			notNull(protocolName)  ,
			notNull(idStrategy)
		);
	}
	
	public final class Implementation implements ComConfiguration, Immutable
	{
		///////////////////////////////////////////////////////////////////////////
		// instance fields //
		////////////////////

		private final PersistenceTypeDictionaryView typeDictionary;
		private final ByteOrder                     byteOrder     ;
		private final String                        version       ;
		private final String                        protocolName  ;
		private final SwizzleIdStrategy             idStrategy    ;
		
		
		
		///////////////////////////////////////////////////////////////////////////
		// constructors //
		/////////////////
		
		Implementation(
			final PersistenceTypeDictionaryView typeDictionary,
			final ByteOrder                     byteOrder     ,
			final String                        version       ,
			final String                        protocolName  ,
			final SwizzleIdStrategy             idStrategy
		)
		{
			super();
			this.typeDictionary = typeDictionary;
			this.byteOrder      = byteOrder     ;
			this.version        = version       ;
			this.protocolName   = protocolName  ;
			this.idStrategy     = idStrategy    ;
		}
		
		
		
		///////////////////////////////////////////////////////////////////////////
		// methods //
		////////////
		
		@Override
		public final PersistenceTypeDictionaryView typeDictionary()
		{
			return this.typeDictionary;
		}

		@Override
		public final ByteOrder byteOrder()
		{
			return this.byteOrder;
		}

		@Override
		public final String version()
		{
			return this.version;
		}

		@Override
		public final String protocolName()
		{
			return this.protocolName;
		}

		@Override
		public final SwizzleIdStrategy idStrategy()
		{
			return this.idStrategy;
		}
		
	}
	
	
	public static ComConfiguration.Assembler Assembler()
	{
		return new ComConfiguration.Assembler.Implementation();
	}
	
	public interface Assembler
	{
		public VarString assembleConfiguration(VarString vs, ComConfiguration configuration);
		
		public default String assembleConfiguration(final ComConfiguration configuration)
		{
			final VarString vs = VarString.New(10_000);
			this.assembleConfiguration(vs, configuration);
			
			return vs.toString();
		}
		
		public final class Implementation implements ComConfiguration.Assembler
		{

			@Override
			public VarString assembleConfiguration(final VarString vs, final ComConfiguration configuration)
			{
				throw new net.jadoth.meta.NotImplementedYetError(); // FIXME ComConfiguration.Assembler#assembleConfiguration()
			}
			
		}
		
	}
	
}
