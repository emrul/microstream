package net.jadoth.persistence.binary.internal;

import java.math.BigInteger;

import net.jadoth.persistence.binary.types.Binary;
import net.jadoth.persistence.types.PersistenceStoreHandler;

public final class BinaryHandlerBigInteger extends AbstractBinaryHandlerNativeCustomValueVariableLength<BigInteger>
{
	///////////////////////////////////////////////////////////////////////////
	// constructors     //
	/////////////////////

	public BinaryHandlerBigInteger()
	{
		super(
			BigInteger.class,
			pseudoFields(
				bytes("value")
			)
		);
	}



	///////////////////////////////////////////////////////////////////////////
	// methods //
	////////////

	@Override
	public void store(final Binary bytes, final BigInteger instance, final long oid, final PersistenceStoreHandler handler)
	{
		bytes.storeArray_byte(this.typeId(), oid, instance.toByteArray());
	}

	@Override
	public BigInteger create(final Binary bytes)
	{
		return new BigInteger(bytes.buildArray_byte());
	}
	
}