package one.microstream.storage.exceptions;

import one.microstream.chars.VarString;
import one.microstream.collections.types.XGettingSequence;

public class StorageExceptionDisruptingExceptions extends StorageException
{
	///////////////////////////////////////////////////////////////////////////
	// instance fields //
	////////////////////
	
	private final XGettingSequence<Throwable> problems;
	
	
	
	///////////////////////////////////////////////////////////////////////////
	// constructors //
	/////////////////

	public StorageExceptionDisruptingExceptions(
		final XGettingSequence<Throwable> problems
	)
	{
		super();
		this.problems = problems;
	}

	public StorageExceptionDisruptingExceptions(
		final XGettingSequence<Throwable> problems,
		final String                      message
	)
	{
		super(message);
		this.problems = problems;
	}

	public StorageExceptionDisruptingExceptions(
		final XGettingSequence<Throwable> problems,
		final Throwable                   cause
	)
	{
		super(cause);
		this.problems = problems;
	}

	public StorageExceptionDisruptingExceptions(
		final XGettingSequence<Throwable> problems,
		final String                      message ,
		final Throwable                   cause
	)
	{
		super(message, cause);
		this.problems = problems;
	}

	public StorageExceptionDisruptingExceptions(
		final XGettingSequence<Throwable> problems          ,
		final String                      message           ,
		final Throwable                   cause             ,
		final boolean                     enableSuppression ,
		final boolean                     writableStackTrace
	)
	{
		super(message, cause, enableSuppression, writableStackTrace);
		this.problems = problems;
	}
	
	
	
	///////////////////////////////////////////////////////////////////////////
	// methods //
	////////////
	
	public final XGettingSequence<Throwable> problems()
	{
		return this.problems;
	}
	
	@Override
	public String assembleOutputString()
	{
		final VarString vs = VarString.New("Disrupting Exceptions:");
		for(final Throwable t : this.problems)
		{
			vs.lf().add(t.getMessage());
		}
		
		return vs.toString();
	}
	
}