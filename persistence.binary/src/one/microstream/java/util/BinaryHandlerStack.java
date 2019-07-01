package one.microstream.java.util;

import java.util.Stack;

import one.microstream.persistence.binary.types.Binary;
import one.microstream.persistence.types.PersistenceLoadHandler;


public final class BinaryHandlerStack extends AbstractBinaryHandlerList<Stack<?>>
{
	///////////////////////////////////////////////////////////////////////////
	// static methods //
	///////////////////

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static Class<Stack<?>> typeWorkaround()
	{
		return (Class)Stack.class; // no idea how to get ".class" to work otherwise
	}
	
	

	///////////////////////////////////////////////////////////////////////////
	// constructors //
	/////////////////

	public BinaryHandlerStack()
	{
		super(typeWorkaround());
	}



	///////////////////////////////////////////////////////////////////////////
	// methods //
	////////////

	@Override
	public final Stack<?> create(final Binary bytes, final PersistenceLoadHandler handler)
	{
		return new Stack<>();
	}

}
