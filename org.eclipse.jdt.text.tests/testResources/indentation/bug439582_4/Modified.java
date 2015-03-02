package indentbug;

public class Bug4
{
	public static <N> Stream<? extends N> test(final N entityPadingPadding,
			final Stream<? extends N> unfilteredStuffPaddingPadding,
			final Object filter)
	{
		final int idI = entityPadingPadding.hashCode();
		return null;
	}
}

interface Predicate<T> {
	boolean test(T t);
}

interface Stream<T> {
	Stream<T> filter(Predicate<? super T> predicate);
}