package one.microstream.jdk8.java.util;

import java.util.Properties;

import one.microstream.X;
import one.microstream.collections.old.KeyValueFlatCollector;
import one.microstream.collections.old.OldCollections;
import one.microstream.memory.sun.SunJdk8Internals;
import one.microstream.persistence.binary.internal.AbstractBinaryHandlerCustomCollection;
import one.microstream.persistence.binary.types.Binary;
import one.microstream.persistence.types.Persistence;
import one.microstream.persistence.types.PersistenceFunction;
import one.microstream.persistence.types.PersistenceLoadHandler;
import one.microstream.persistence.types.PersistenceReferenceLoader;
import one.microstream.persistence.types.PersistenceStoreHandler;


public final class BinaryHandlerProperties extends AbstractBinaryHandlerCustomCollection<Properties>
{
	///////////////////////////////////////////////////////////////////////////
	// constants //
	//////////////

	// no load factor because the Properties class does not allow to specify one. It is always the Hashtable default.
	static final long BINARY_OFFSET_DEFAULTS =                                                    0;
	static final long BINARY_OFFSET_ELEMENTS = BINARY_OFFSET_DEFAULTS + Binary.objectIdByteLength();



	///////////////////////////////////////////////////////////////////////////
	// static methods //
	///////////////////

	private static Class<Properties> typeWorkaround()
	{
		return Properties.class;
	}

	static final int getElementCount(final Binary bytes)
	{
		return X.checkArrayRange(bytes.getListElementCountKeyValue(BINARY_OFFSET_ELEMENTS));
	}
	
	public static BinaryHandlerProperties New()
	{
		return new BinaryHandlerProperties();
	}



	///////////////////////////////////////////////////////////////////////////
	// constructors //
	/////////////////

	BinaryHandlerProperties()
	{
		super(
			typeWorkaround(),
			keyValuesFields(
				CustomField(Properties.class, "defaults")
			)
		);
	}



	///////////////////////////////////////////////////////////////////////////
	// methods //
	////////////

	@Override
	public final void store(
		final Binary                  bytes   ,
		final Properties              instance,
		final long                    objectId,
		final PersistenceStoreHandler handler
	)
	{
		// store elements simply as array binary form
		bytes.storeMapEntrySet(
			this.typeId()         ,
			objectId              ,
			BINARY_OFFSET_ELEMENTS,
			instance.entrySet()   ,
			handler
		);

		bytes.store_long(
			BINARY_OFFSET_DEFAULTS,
			handler.apply(SunJdk8Internals.accessDefaults(instance))
		);
	}
	

	@Override
	public final Properties create(final Binary data, final PersistenceLoadHandler idResolver)
	{
		return new Properties();
	}

	@Override
	public final void updateState(
		final Binary                 data    ,
		final Properties             instance,
		final PersistenceLoadHandler handler
	)
	{
		instance.clear();
		
		final Object defaults = handler.lookupObject(data.read_long(BINARY_OFFSET_DEFAULTS));
		
		// the cast is important to ensure the type validity of the resolved defaults instance.
		SunJdk8Internals.setDefaults(instance, (Properties)defaults);
		
		final int elementCount = getElementCount(data);
		final KeyValueFlatCollector<Object, Object> collector = KeyValueFlatCollector.New(elementCount);
		data.collectKeyValueReferences(BINARY_OFFSET_ELEMENTS, elementCount, handler, collector);
		data.registerHelper(instance, collector.yield());
	}

	@Override
	public void complete(final Binary data, final Properties instance, final PersistenceLoadHandler handler)
	{
		OldCollections.populateMapFromHelperArray(instance, data.getHelper(instance));
	}

	@Override
	public final void iterateInstanceReferences(final Properties instance, final PersistenceFunction iterator)
	{
		iterator.apply(SunJdk8Internals.accessDefaults(instance));
		Persistence.iterateReferencesMap(iterator, instance);
	}
	
	@Override
	public final void iterateLoadableReferences(final Binary data, final PersistenceReferenceLoader iterator)
	{
		iterator.acceptObjectId(data.read_long(BINARY_OFFSET_DEFAULTS));
		data.iterateKeyValueEntriesReferences(BINARY_OFFSET_ELEMENTS, iterator);
	}
	
}
