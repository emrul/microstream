package net.jadoth.storage.types;

import net.jadoth.persistence.binary.types.ChunksBuffer;
import net.jadoth.persistence.types.PersistenceIdSet;

public interface StorageRequestTaskLoadByOids extends StorageRequestTaskLoad
{
	public final class Implementation extends StorageRequestTaskLoad.AbstractImplementation
	implements StorageRequestTaskLoadByOids, StorageChannelTaskLoadByOids
	{
		///////////////////////////////////////////////////////////////////////////
		// instance fields //
		////////////////////

		private final PersistenceIdSet[] oidList;



		///////////////////////////////////////////////////////////////////////////
		// constructors //
		/////////////////

		Implementation(final long timestamp, final PersistenceIdSet[] oidList)
		{
			/* (16.01.2014 TM)NOTE:
			 * using calculateRequiredProgress() here is a clear bug as a lower progress count (e.g. 1)
			 * does absolutely not guarantee that the processing channel(s) only have lower channel indices (e.g. 0)
			 * Absolutely astonishing that this worked correctly thousands of times in the last year and causes
			 * a problem just now.
			 */
			super(timestamp, oidList.length);
//			super(calculateRequiredProgress(oidList));
			this.oidList = oidList;
		}



		///////////////////////////////////////////////////////////////////////////
		// methods //
		////////////

		@Override
		protected final ChunksBuffer internalProcessBy(final StorageChannel channel)
		{
			return channel.collectLoadByOids(this.resultArray(), this.oidList[channel.channelIndex()]);
		}

	}

}