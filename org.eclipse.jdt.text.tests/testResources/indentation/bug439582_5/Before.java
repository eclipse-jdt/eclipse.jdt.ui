package indentbug1;

public class Bug5
{
	public static <N> Object test(final N entityPadingPadding,
	                                           final Stream<? extends N> unfilteredStuffPaddingPadding,
			final Object filter)
	                                           {
		final int idI = entityPadingPadding.hashCode();
		return unfilteredStuffPaddingPadding.filter(new Predicate<N>()
		                                                     {
			@Override
			public boolean test(final N t)
			{
				return filter.hashCode() == idI;
			}
		                                                     });
	                                           }
}

interface Predicate<T> {
	boolean test(T t);
}

interface Stream<T> {
	 Stream<T> filter(Predicate<? super T> predicate);
}