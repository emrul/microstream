package net.jadoth.storage.types;

import static net.jadoth.X.notNull;

import java.io.File;

import net.jadoth.files.XFiles;
import net.jadoth.persistence.types.PersistenceTypeDefinition;


public interface StorageEntityTypeConversionFileProvider
{
	public StorageLockedFile provideConversionFile(PersistenceTypeDefinition typeDescription, StorageFile sourceFile);



	public final class Implementation implements StorageEntityTypeConversionFileProvider
	{
		///////////////////////////////////////////////////////////////////////////
		// instance fields //
		////////////////////

		private final File   directory ;
		private final String fileSuffix;

		private final transient String cachedFileSuffix;



		///////////////////////////////////////////////////////////////////////////
		// constructors //
		/////////////////

		public Implementation(final File directory, final String fileSuffix)
		{
			super();
			this.directory = notNull(directory);
			this.fileSuffix = fileSuffix;
			this.cachedFileSuffix = fileSuffix == null ? "" : '.' + fileSuffix;
		}



		///////////////////////////////////////////////////////////////////////////
		// declared methods //
		/////////////////////

		public final String fileSuffix()
		{
			return this.fileSuffix;
		}



		///////////////////////////////////////////////////////////////////////////
		// methods //
		////////////

		@Override
		public StorageLockedFile provideConversionFile(
			final PersistenceTypeDefinition typeDescription,
			final StorageFile               sourceFile
		)
		{
			// TypeId must be included since only that is the unique identifier of a type.
			final File file = new File(
				this.directory, typeDescription.typeName() + "_" + typeDescription.typeId() + this.cachedFileSuffix
			);
			XFiles.ensureDirectory(this.directory);
			
			return StorageLockedFile.openLockedFile(file);
		}

	}

}