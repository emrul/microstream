package net.jadoth.persistence.types;

import static net.jadoth.X.notNull;

import java.util.function.Consumer;

import net.jadoth.persistence.exceptions.PersistenceExceptionTypeNotPersistable;

/**
 * Named "ensurer" because depending on the case, it creates a new type handler or it just returns
 * already existing, pre-registered ones. So "ensuring" is the most fitting common denominator.
 * 
 * @author TM
 */
public interface PersistenceTypeHandlerEnsurer<M> extends PersistenceTypeHandlerIterable<M>
{
	public <T> PersistenceTypeHandler<M, T> ensureTypeHandler(Class<T> type)
		throws PersistenceExceptionTypeNotPersistable;
	
	
	public static <M> PersistenceTypeHandlerEnsurer.Implementation<M> New(
		final PersistenceCustomTypeHandlerRegistry<M> customTypeHandlerRegistry,
		final PersistenceTypeHandlerCreator<M>        typeHandlerCreator
	)
	{
		return new PersistenceTypeHandlerEnsurer.Implementation<>(
			notNull(customTypeHandlerRegistry),
			notNull(typeHandlerCreator)
		);
	}
	
	

	public class Implementation<M> implements PersistenceTypeHandlerEnsurer<M>
	{
		///////////////////////////////////////////////////////////////////////////
		// instance fields  //
		/////////////////////

		final PersistenceCustomTypeHandlerRegistry<M> customTypeHandlerRegistry;
		final PersistenceTypeHandlerCreator<M>        typeHandlerCreator       ;



		///////////////////////////////////////////////////////////////////////////
		// constructors     //
		/////////////////////

		Implementation(
			final PersistenceCustomTypeHandlerRegistry<M> customTypeHandlerRegistry,
			final PersistenceTypeHandlerCreator<M>        typeHandlerCreator
		)
		{
			super();
			this.customTypeHandlerRegistry = customTypeHandlerRegistry;
			this.typeHandlerCreator        = typeHandlerCreator       ;
		}



		///////////////////////////////////////////////////////////////////////////
		// declared methods //
		/////////////////////

		@Override
		public <T> PersistenceTypeHandler<M, T> ensureTypeHandler(final Class<T> type)
			throws PersistenceExceptionTypeNotPersistable
		{
			// lookup predefined handler first to cover primitives and to give custom handlers precedence
			final PersistenceTypeHandler<M, T> customHandler = this.customTypeHandlerRegistry.lookupTypeHandler(type);
			if(customHandler != null)
			{
				return customHandler;
			}
			
			return this.typeHandlerCreator.createTypeHandler(type);
		}
		
		@Override
		public <C extends Consumer<? super PersistenceTypeHandler<M, ?>>> C iterateTypeHandlers(final C iterator)
		{
			return this.customTypeHandlerRegistry.iterateTypeHandlers(iterator);
		}

	}

}