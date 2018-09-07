package net.jadoth.persistence.types;

public interface PersistenceTypeDescriptionMemberField extends PersistenceTypeDescriptionMember
{
	public String declaringTypeName();

	@Override
	public String uniqueName();
	

	// (14.08.2015 TM)TODO: include Generics, Field#getGenericType
//	public String typeParamterString();


	@Override
	public default boolean equalsDescription(final PersistenceTypeDescriptionMember member)
	{
		return member instanceof PersistenceTypeDescriptionMemberField
			&& equalDescription(this, (PersistenceTypeDescriptionMemberField)member)
		;
	}
	
	public static boolean equalDescription(
		final PersistenceTypeDescriptionMemberField m1,
		final PersistenceTypeDescriptionMemberField m2
	)
	{
		return PersistenceTypeDescriptionMember.equalDescription(m1, m2)
			&& m1.declaringTypeName().equals(m2.declaringTypeName())
		;
	}
	
	

	public static PersistenceTypeDescriptionMemberField New(
		final String  typeName               ,
		final String  name                   ,
		final String  declaringTypeName      ,
		final boolean isReference            ,
		final long    persistentMinimumLength,
		final long    persistentMaximumLength
	)
	{
		return new PersistenceTypeDescriptionMemberField.Implementation(
			typeName               ,
			name                   ,
			declaringTypeName      ,
			isReference            ,
			persistentMinimumLength,
			persistentMaximumLength
		);
	}

	public final class Implementation
	extends PersistenceTypeDescriptionMember.AbstractImplementation
	implements PersistenceTypeDescriptionMemberField
	{
		///////////////////////////////////////////////////////////////////////////
		// instance fields //
		////////////////////

		private final           String declaringTypeName ;
		private final transient String qualifiedFieldName;



		///////////////////////////////////////////////////////////////////////////
		// constructors //
		/////////////////

		protected Implementation(
			final String  typeName           ,
			final String  name               ,
			final String  declaringTypeName  ,
			final boolean isReference        ,
			final long    persistentMinLength,
			final long    persistentMaxLength
		)
		{
			super(typeName, name, isReference, !isReference, false, isReference, persistentMinLength, persistentMaxLength);
			this.declaringTypeName  = declaringTypeName;
			this.qualifiedFieldName = PersistenceTypeDictionary.fullQualifiedFieldName(declaringTypeName, name);
		}



		///////////////////////////////////////////////////////////////////////////
		// methods //
		////////////

		@Override
		public String declaringTypeName()
		{
			return this.declaringTypeName;
		}

		@Override
		public String uniqueName()
		{
			return this.qualifiedFieldName;
		}

		@Override
		public void assembleTypeDescription(final Appender assembler)
		{
			assembler.appendTypeMemberDescription(this);
		}



	}

}
