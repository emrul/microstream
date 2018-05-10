package net.jadoth.persistence.types;

import static net.jadoth.X.notNull;

import net.jadoth.collections.types.XGettingSequence;
import net.jadoth.collections.types.XImmutableSequence;
import net.jadoth.hashing.HashEqualator;
import net.jadoth.swizzling.types.SwizzleTypeIdentity;
import net.jadoth.swizzling.types.SwizzleTypeLink;

public interface PersistenceTypeDefinition<T> extends PersistenceTypeDictionaryEntry, SwizzleTypeLink<T>
{
	@Override
	public long   typeId();

	@Override
	public String typeName();

	/* (30.06.2015 TM)TODO: PersistenceTypeDescription <?>Generics
	 * Must consider Generics Type information as well, at least as a simple normalized String for
	 * equality comparison.
	 * Otherwise, changing type parameter won't be recognized by the type validation and
	 * loading/building of entities will result in heap pollution (wrong instance for the type).
	 * Example:
	 * Lazy<Person> changed to Lazy<Employee>.
	 * Currently, this is just recognized as Lazy.
	 * 
	 * (05.04.2017 TM)NOTE: but does it really have to be stored here?
	 * Wouldn't it be enough to store it in the member description?
	 * E.g. Type "Lazy" PLUS type parameter "[full qualified] Person"
	 */

	@Override
	public XGettingSequence<? extends PersistenceTypeDescriptionMember> members();

	public boolean hasPersistedReferences();

	/**
	 * Provides information if two instances of the handled type can have different length in persisted form.<p>
	 * Examples for variable length types:
	 * <ul>
	 * <li> arrays</li>
	 * <li>{@code java.lang.String}</li>
	 * <li>{@code java.util.ArrayList}</li>
	 * <li>{@code java.math.BigDecimal}</li>
	 * </ul><p>
	 * Examples for fixed length types:
	 * <ul>
	 * <li>primitive value wrapper types</li>
	 * <li>{@code java.lang.Object}</li>
	 * <li>{@code java.util.Date}</li>
	 * <li>typical entity types (without unshared inlined variable length component instances)</li>
	 * </ul>
	 *
	 * @return
	 */
	public boolean hasPersistedVariableLength();

	public boolean isPrimitiveType();
	
	/**
	 * Provides information if one particular instance can have variing binary length from one store to another.<p>
	 * Examples for variable length instances:
	 * <ul>
	 * <li> variable size collection instances</li>
	 * <li> variable size pesudo collection instances like {@code java.util.StringBuilder}</li>
	 * <li> instances of custom defined types similar to collections</li>
	 * </ul><p>
	 * Examples for fixed length instances:
	 * <ul>
	 * <li>arrays</li>
	 * <li>all immutable type instances (like {@code java.lang.String} )</li>
	 * <li>all fixed length types (see {@link #isVariableBinaryLengthType()}</li>
	 * </ul>
	 *
	 * @return
	 */
	public boolean hasVaryingPersistedLengthInstances();
	
	public default String toTypeString()
	{
		return this.getClass().getName() + "<" + this.type().getName() + ">(TID " + this.typeId() + ")";
	}



	public static boolean determineVariableLength(
		final XGettingSequence<? extends PersistenceTypeDescriptionMember> members
	)
	{
		for(final PersistenceTypeDescriptionMember member : members)
		{
			if(member.isVariableLength())
			{
				return true;
			}
		}
		return false;
	}

	public static boolean determineIsPrimitive(
		final XGettingSequence<? extends PersistenceTypeDescriptionMember> members
	)
	{
		return members.size() == 1 && members.get().isPrimitiveDefinition();
	}

	public static final HashEqualator<PersistenceTypeDefinition<?>> EQUAL_TYPE =
		new HashEqualator<PersistenceTypeDefinition<?>>()
		{
			@Override
			public int hash(final PersistenceTypeDefinition<?> typeDescription)
			{
				return PersistenceTypeDefinition.hashCode(typeDescription);
			}

			@Override
			public boolean equal(final PersistenceTypeDefinition<?> td1, final PersistenceTypeDefinition<?> td2)
			{
				return PersistenceTypeDefinition.equalType(td1, td2);
			}
		}
	;



	public static int hashCode(final PersistenceTypeDefinition <?>typeDescription)
	{
		return Long.hashCode(typeDescription.typeId()) & typeDescription.typeName().hashCode();
	}

	public static boolean equalType(
		final PersistenceTypeDefinition <?>td1,
		final PersistenceTypeDefinition <?>td2
	)
	{
		if(td1 == td2)
		{
			return true;
		}
		if(td1 == null)
		{
			return td2 == null;
		}
		if(td2 == null)
		{
			return false;
		}
		
		return SwizzleTypeIdentity.equals(td1, td2);
	}

	public static boolean equalDescription(
		final PersistenceTypeDefinition <?>td1,
		final PersistenceTypeDefinition <?>td2
	)
	{
		if(td1 == td2)
		{
			return true;
		}
		if(td1 == null)
		{
			return td2 == null;
		}
		if(td2 == null)
		{
			return false;
		}
		
		return SwizzleTypeIdentity.equals(td1, td2)
			&& PersistenceTypeDescriptionMember.equalMembers(td1.members(), td2.members())
		;
	}

	
	
	public static <T> PersistenceTypeDefinition<T> New(
		final String                                                       typeName,
		final Class<T>                                                     type    ,
		final long                                                         typeId  ,
		final XGettingSequence<? extends PersistenceTypeDescriptionMember> members
	)
	{
		return new PersistenceTypeDefinition.Implementation<>(typeId, typeName, type, members);
	}



	public final class Implementation<T>
	extends PersistenceTypeDictionaryEntry.AbstractImplementation
	implements PersistenceTypeDefinition<T>
	{
		///////////////////////////////////////////////////////////////////////////
		// instance fields //
		////////////////////

		final long                                                           typeId        ;
		final String                                                         typeName      ;
		final Class<T>                                                       type          ;
		final XImmutableSequence<? extends PersistenceTypeDescriptionMember> members       ;
		final boolean                                                        hasReferences ;
		final boolean                                                        isPrimitive   ;
		final boolean                                                        variableLength;



		///////////////////////////////////////////////////////////////////////////
		// constructors //
		/////////////////

		Implementation(
			final long                                                         typeId  ,
			final String                                                       typeName,
			final Class<T>                                                     type    ,
			final XGettingSequence<? extends PersistenceTypeDescriptionMember> members
		)
		{
			super();
			this.typeId         =         typeId   ;
			this.typeName       = notNull(typeName);
			this.type           =         type     ; // may be null for obsolete type description or external process
			this.members        = members.immure() ; // same instance if already immutable
			this.hasReferences  = PersistenceTypeDescriptionMember.determineHasReferences (members);
			this.isPrimitive    = PersistenceTypeDefinition      .determineIsPrimitive   (members);
			this.variableLength = PersistenceTypeDefinition      .determineVariableLength(members);
		}

		

		///////////////////////////////////////////////////////////////////////////
		// methods //
		////////////

		@Override
		public final long typeId()
		{
			return this.typeId;
		}
		
		@Override
		public final String typeName()
		{
			return this.typeName;
		}
		
		@Override
		public final Class<T> type()
		{
			return this.type;
		}
		
		@Override
		public final XImmutableSequence<? extends PersistenceTypeDescriptionMember> members()
		{
			return this.members;
		}

		@Override
		public final boolean hasPersistedReferences()
		{
			return this.hasReferences;
		}

		@Override
		public final boolean isPrimitiveType()
		{
			return this.isPrimitive;
		}

		@Override
		public final boolean hasPersistedVariableLength()
		{
			return this.variableLength;
		}
		
		@Override
		public final boolean hasVaryingPersistedLengthInstances()
		{
			return this.variableLength;
		}
		
		@Override
		public final String toString()
		{
			return this.toTypeString();
		}
		

	}
	
}
