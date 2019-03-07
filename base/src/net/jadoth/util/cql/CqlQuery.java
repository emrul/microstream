package net.jadoth.util.cql;

import static net.jadoth.X.notNull;

import java.util.Comparator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import net.jadoth.collections.ArrayCollector;
import net.jadoth.collections.types.XIterable;
import net.jadoth.collections.types.XSequence;
import net.jadoth.functional.Aggregator;

public interface CqlQuery<I, O, R>
{
	// fluent API //

	public default CqlQuery<I, O, R> skip(final Number count) // Number to allow fluent API use of int values
	{
		return CqlQuery.New(
			this.getSource()   ,
			CQL.asLong(count) ,
			this.getLimit()    ,
			this.getSelector() ,
			this.getProjector(),
			this.getOrder()    ,
			this.getResultor()
		);
	}

	public default CqlQuery<I, O, R> limit(final Number count) // Number to allow fluent API use of int values
	{
		return CqlQuery.New(
			this.getSource()   ,
			this.getSkip()     ,
			CQL.asLong(count) ,
			this.getSelector() ,
			this.getProjector(),
			this.getOrder()    ,
			this.getResultor()
		);
	}

	public default CqlQuery<I, O, R> select(final Predicate<? super I> selector)
	{
		return CqlQuery.New(
			this.getSource()   ,
			this.getSkip()     ,
			this.getLimit()    ,
			selector           ,
			this.getProjector(),
			this.getOrder()    ,
			this.getResultor()
		);
	}

	public default CqlQuery<I, O, R> orderBy(final Comparator<? super O> order)
	{
		return CqlQuery.New(
			this.getSource()   ,
			this.getSkip()     ,
			this.getLimit()    ,
			this.getSelector() ,
			this.getProjector(),
			order              ,
			this.getResultor()
		);
	}

	public default CqlQuery<I, O, R> from(final XIterable<? extends I> source)
	{
		return CqlQuery.New(
			source             ,
			this.getSkip()     ,
			this.getLimit()    ,
			this.getSelector() ,
			this.getProjector(),
			this.getOrder()    ,
			this.getResultor()
		);
	}

	public default <P> CqlProjection<I, P> project(final Function<? super I, P> projector)
	{
		return CqlProjection.New(
			this.getSource()   ,
			this.getSkip()     ,
			this.getLimit()    ,
			this.getSelector() ,
			projector          ,
			null
		);
	}

	public default <P extends Consumer<O> & XIterable<O>> CqlIteration<I, O, P> into(final Supplier<P> supplier)
	{
		return CqlIteration.New(
			this.getSource()    ,
			this.getSkip()      ,
			this.getLimit()     ,
			this.getSelector()  ,
			this.getProjector() ,
			this.getOrder()     ,
			CqlResultor.NewFromSupplier(supplier)
		);
	}

	public default <P extends Consumer<O> & XIterable<O>> CqlIteration<I, O, P> into(final P target)
	{
		return CqlIteration.New(
			this.getSource()    ,
			this.getSkip()      ,
			this.getLimit()     ,
			this.getSelector()  ,
			this.getProjector() ,
			this.getOrder()     ,
			CqlResultor.New(target)
		);
	}

	public default <X extends XIterable<O>> CqlIteration<I, O, X> into(final CqlResultor<O, X> resultor)
	{
		return CqlIteration.New(
			this.getSource()    ,
			this.getSkip()      ,
			this.getLimit()     ,
			this.getSelector()  ,
			this.getProjector() ,
			this.getOrder()     ,
			notNull(resultor)
		);
	}

	public default <R1> CqlQuery<I, O, R1> over(final CqlResultor<O, R1> resultor)
	{
		return CqlQuery.New(
			this.getSource()   ,
			this.getSkip()     ,
			this.getLimit()    ,
			this.getSelector() ,
			this.getProjector(),
			this.getOrder()    ,
			notNull(resultor)
		);
	}

	public default <R1> CqlQuery<I, O, R1> targeting(final Aggregator<O, R1> collector)
	{
		return CqlQuery.New(
			this.getSource()   ,
			this.getSkip()     ,
			this.getLimit()    ,
			this.getSelector() ,
			this.getProjector(),
			this.getOrder()    ,
			CqlResultor.NewFromAggregator(collector)
		);
	}

	// getter //

	public default Long getSkip()
	{
		return null;
	}

	public default Long getLimit()
	{
		return null;
	}

	public default XIterable<? extends I> getSource()
	{
		return null;
	}

	public default Predicate<? super I> getSelector()
	{
		return null;
	}

	public Function<? super I, O> getProjector();

	public CqlResultor<O, R> getResultor();

	public default Comparator<? super O> getOrder()
	{
		return null;
	}

	// execution //

	public default R execute()
	{
		return this.executeOn(CQL.prepareSource(this.getSource()));
	}

	public default R executeOn(final XIterable<? extends I> source)
	{
		return CQL.executeQuery(
			source             ,
			this.getSkip()     ,
			this.getLimit()    ,
			this.getSelector() ,
			this.getProjector(),
			this.getResultor() ,
			this.getOrder()
		);
	}

	public default <P extends Consumer<O>> P executeInto(final P target)
	{
		return this.executeInto(CQL.prepareSource(this.getSource()), target);
	}

	public default O[] executeInto(final O[] target)
	{
		return this.executeInto(target, 0);
	}

	public default O[] executeInto(final O[] target, final int size)
	{
		return this.executeInto(CQL.prepareSource(this.getSource()), new ArrayCollector<>(target, size)).getArray();
	}

	public default <P extends Consumer<I>> P executeSelection(final XIterable<? extends I> source, final P target)
	{
		return CQL.executeQuery(source, this.getSkip(), this.getLimit(), this.getSelector(), target);
	}

	public default <P extends Consumer<O>> P executeInto(final XIterable<? extends I> source, final P target)
	{
		return CQL.executeQuery(
			source             ,
			this.getSkip()     ,
			this.getLimit()    ,
			this.getSelector() ,
			this.getProjector(),
			target             ,
			this.getOrder()
		);
	}

	// constructors //

	public static <I, O, R> CqlQuery<I, O, R> New()
	{
		return new Implementation<>(null, null, null, null, null, null, null);
	}

	public static <I, O> CqlQuery<I, O, XSequence<O>> New(
		final XIterable<? extends I> source    ,
		final Long                   skip      ,
		final Long                   limit     ,
		final Predicate<? super I>   selector  ,
		final Function<? super I, O> projector ,
		final Comparator<? super O>  comparator
	)
	{
		return new Implementation<>(
			source            ,
			skip              ,
			limit             ,
			selector          ,
			notNull(projector),
			comparator        ,
			CqlResultor.New()
		);
	}

	public static <I, O, R> CqlQuery<I, O, R> New(
		final XIterable<? extends I> source    ,
		final Long                   skip      ,
		final Long                   limit     ,
		final Predicate<? super I>   selector  ,
		final Function<? super I, O> projector ,
		final Comparator<? super O>  comparator,
		final Aggregator<O, R>       aggregator
	)
	{
		return new Implementation<>(
			source                  ,
			skip                    ,
			limit                   ,
			selector                ,
			projector               ,
			comparator              ,
			CqlResultor.NewFromAggregator(aggregator)
		);
	}

	public static <I, O, R extends Consumer<O> & XIterable<O>> CqlQuery<I, O, R> New(
		final XIterable<? extends I> source    ,
		final Long                   skip      ,
		final Long                   limit     ,
		final Predicate<? super I>   selector  ,
		final Function<? super I, O> projector ,
		final Comparator<? super O>  comparator,
		final R                      target
	)
	{
		return new Implementation<>(
			source                 ,
			skip                   ,
			limit                  ,
			selector               ,
			projector              ,
			comparator             ,
			CqlResultor.New(target)
		);
	}

	public static <I, O, R> CqlQuery<I, O, R> New(
		final XIterable<? extends I> source    ,
		final Long                   skip      ,
		final Long                   limit     ,
		final Predicate<? super I>   selector  ,
		final Function<? super I, O> projector ,
		final Comparator<? super O>  comparator,
		final CqlResultor<O, R>      resultor
	)
	{
		return new Implementation<>(source, skip, limit, selector, projector, comparator, resultor);
	}

	// implementations //

	abstract class Abstract<I, O, R> implements CqlQuery<I, O, R>
	{
		///////////////////////////////////////////////////////////////////////////
		// instance fields  //
		/////////////////////

		final XIterable<? extends I> source    ;
		final Long                   skip      ;
		final Long                   limit     ;
		final Predicate<? super I>   selector  ;
		final Function<? super I, O> projector ;
		final CqlResultor<O, R>      resultor  ;
		final Comparator<? super O>  comparator;



		///////////////////////////////////////////////////////////////////////////
		// declared methods //
		/////////////////////

		Abstract(
			final XIterable<? extends I> source    ,
			final Long                   skip      ,
			final Long                   limit     ,
			final Predicate<? super I>   selector  ,
			final Function<? super I, O> projector ,
			final Comparator<? super O>  comparator,
			final CqlResultor<O, R>         resultor
		)
		{
			// every field may be null to support fluent API (and most are optional anyway)
			super();
			this.source     = source    ;
			this.skip       = skip      ;
			this.limit      = limit     ;
			this.selector   = selector  ;
			this.projector  = projector ;
			this.resultor   = resultor  ;
			this.comparator = comparator;
		}



		///////////////////////////////////////////////////////////////////////////
		// methods //
		////////////

		@Override
		public final Long getSkip()
		{
			return this.skip;
		}

		@Override
		public final Long getLimit()
		{
			return this.limit;
		}

		@Override
		public final XIterable<? extends I> getSource()
		{
			return this.source;
		}

		@Override
		public final Predicate<? super I> getSelector()
		{
			return this.selector;
		}

		@Override
		public Function<? super I, O> getProjector()
		{
			return this.projector;
		}

		@Override
		public final Comparator<? super O> getOrder()
		{
			return this.comparator;
		}

		@Override
		public final CqlResultor<O, R> getResultor()
		{
			return this.resultor;
		}

	}

	final class Implementation<I, O, R> extends Abstract<I, O, R>
	{
		Implementation(
			final XIterable<? extends I> source    ,
			final Long                   skip      ,
			final Long                   limit     ,
			final Predicate<? super I>   selector  ,
			final Function<? super I, O> projector ,
			final Comparator<? super O>  comparator,
			final CqlResultor<O, R>         resultor
		)
		{
			super(source, skip, limit, selector, projector, comparator, resultor);
		}

	}

}