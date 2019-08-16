package one.microstream.persistence.binary.internal;

import one.microstream.collections.BulkList;
import one.microstream.collections.ConstHashEnum;
import one.microstream.collections.types.XGettingEnum;
import one.microstream.collections.types.XGettingSequence;
import one.microstream.persistence.binary.types.Binary;
import one.microstream.persistence.types.Persistence;
import one.microstream.persistence.types.PersistenceObjectIdResolver;
import one.microstream.persistence.types.PersistenceTypeDefinitionMember;
import one.microstream.persistence.types.PersistenceTypeDefinitionMemberEnumConstant;
import one.microstream.reflect.XReflect;

public abstract class AbstractBinaryHandlerCustomEnum<T extends Enum<T>> extends AbstractBinaryHandlerCustom<T>
{
	///////////////////////////////////////////////////////////////////////////
	// static methods //
	///////////////////
	
	public static <T> Class<T> validateIsEnum(final Class<T> type)
	{
		if(XReflect.isEnum(type))
		{
			return type;
		}

		// (16.08.2019 TM)EXCP: proper exception
		throw new IllegalArgumentException("Not an Enum type: " + type.getName());
	}
	
	
	
	///////////////////////////////////////////////////////////////////////////
	// instance fields //
	////////////////////
	
	final ConstHashEnum<PersistenceTypeDefinitionMemberEnumConstant> constantMembers;
	final ConstHashEnum<PersistenceTypeDefinitionMember>             allMembers     ;
	
	
	
	///////////////////////////////////////////////////////////////////////////
	// constructors //
	/////////////////

	protected AbstractBinaryHandlerCustomEnum(
		final Class<T>                                                                type           ,
		final XGettingSequence<? extends PersistenceTypeDefinitionMemberEnumConstant> constantMembers,
		final XGettingSequence<? extends PersistenceTypeDefinitionMember>             instanceMembers
	)
	{
		this(type, deriveTypeName(type), constantMembers, instanceMembers);
	}
	
	protected AbstractBinaryHandlerCustomEnum(
		final Class<T>                                                                type           ,
		final String                                                                  typeName       ,
		final XGettingSequence<? extends PersistenceTypeDefinitionMemberEnumConstant> constantMembers,
		final XGettingSequence<? extends PersistenceTypeDefinitionMember>             instanceMembers
	)
	{
		super(validateIsEnum(type), typeName, instanceMembers);
		this.constantMembers = ConstHashEnum.New(constantMembers);
		this.allMembers      = ConstHashEnum.New(
			BulkList.<PersistenceTypeDefinitionMember>New(constantMembers).addAll(instanceMembers)
		);
	}

	
	
	///////////////////////////////////////////////////////////////////////////
	// methods //
	////////////
	
	@Override
	public Object[] collectEnumConstants()
	{
		// legacy type handlers return null here to indicate their root entry is obsolete
		return Persistence.collectEnumConstants(this);
	}
	
	@Override
	public final XGettingEnum<? extends PersistenceTypeDefinitionMember> allMembers()
	{
		return this.allMembers;
	}
	
	protected abstract int getOrdinal(Binary bytes);
	
	protected abstract String getName(Binary bytes, PersistenceObjectIdResolver idResolver);
	
	@Override
	public T create(final Binary bytes, final PersistenceObjectIdResolver idResolver)
	{
		// copied from BinaryHandlerEnum#create
		
		// Class detour required for AIC-like special subclass enums constants.
		final Object[] jvmEnumConstants = XReflect.getDeclaredEnumClass(this.type()).getEnumConstants();
		final int persistentOrdinal     = this.getOrdinal(bytes);
		
		/*
		 * Can't validate here since the name String instance might not have been created, yet. See #update.
		 * Nevertheless:
		 * - the enum constants storing order must be assumed to be consistent with the type dictionary constants names.
		 * - the type dictionary constants names are validated against the current runtime type.
		 * These two aspects in combination ensure that the correct enum constant instance is selected.
		 * 
		 * Mismatches between persistent form and runtime type must be handled via a LegacyTypeHandler, not here.
		 */
		
		/*
		 * Required for AIC-like special subclass enums constants:
		 * The instance is actually of type T, but it is stored in a "? super T" array of it parent enum type.
		 */
		@SuppressWarnings("unchecked")
		final T enumConstantinstance = (T)jvmEnumConstants[persistentOrdinal];
		
		return enumConstantinstance;
	}
	
	protected void validate(
		final Binary                      bytes     ,
		final T                           instance  ,
		final PersistenceObjectIdResolver idResolver
	)
	{
		// validate ordinal, just in case.
		final int persistentOrdinal = this.getOrdinal(bytes);
		if(persistentOrdinal != instance.ordinal())
		{
			// (01.08.2019 TM)EXCP: proper exception
			throw new RuntimeException(
				"Inconcistency for " + instance.getDeclaringClass().getName() + "." + instance.name()
			);
		}
		
		final String persistentName = this.getName(bytes, idResolver);
		if(!instance.name().equals(persistentName))
		{
			// (09.08.2019 TM)EXCP: proper exception
			throw new RuntimeException(
				"Enum constant inconsistency:"
				+ " in type " + this.type().getName()
				+ " persisted instance with ordinal " + persistentOrdinal + ", name \"" + persistentName + "\""
				+ " does not match"
				+ " JVM-created instance with ordinal " + instance.ordinal() + ", name \"" + instance.name() + "\""
			);
		}
	}
		
	@Override
	public void update(final Binary bytes, final T instance, final PersistenceObjectIdResolver idResolver)
	{
		// must thoroughly validate the linked jvm-generated(!) instance before modifying its state!
		this.validate(bytes, instance, idResolver);
		
		// super class logic is currently no-op, but is called here for future consistency.
		super.update(bytes, instance, idResolver);
	}
	
}
