package one.microstream.persistence.binary.one.microstream.entity;

/*-
 * #%L
 * microstream-persistence-binary
 * %%
 * Copyright (C) 2019 - 2021 MicroStream Software
 * %%
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 * 
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License, v. 2.0 are satisfied: GNU General Public License, version 2
 * with the GNU Classpath Exception which is
 * available at https://www.gnu.org/software/classpath/license.html.
 * 
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 * #L%
 */

import static one.microstream.X.notNull;

import one.microstream.persistence.binary.types.Binary;
import one.microstream.persistence.types.PersistenceFunction;
import one.microstream.persistence.types.PersistenceStoreHandler;

public interface EntityPersister extends PersistenceFunction
{
	public static EntityPersister New(
		final EntityTypeHandlerManager        entityTypeHandlerManager,
		final PersistenceStoreHandler<Binary> handler
	)
	{
		return new Default(
			notNull(entityTypeHandlerManager),
			notNull(handler)
		);
	}
	
	
	public static class Default implements EntityPersister
	{
		private final EntityTypeHandlerManager        entityTypeHandlerManager;
		private final PersistenceStoreHandler<Binary> handler                 ;
		
		Default(
			final EntityTypeHandlerManager        entityTypeHandlerManager,
			final PersistenceStoreHandler<Binary> handler
		)
		{
			super();
			this.entityTypeHandlerManager = entityTypeHandlerManager;
			this.handler                  = handler                 ;
		}

		@Override
		public <T> long apply(final T instance)
		{
			return this.handler.applyEager(
				instance,
				this.entityTypeHandlerManager.ensureInternalEntityTypeHandler(instance)
			);
		}
		
	}
	
}
