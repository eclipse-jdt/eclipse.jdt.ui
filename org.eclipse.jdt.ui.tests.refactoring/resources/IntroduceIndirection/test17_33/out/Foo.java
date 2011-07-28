package p;
class C {
}

interface I {
}

public class Foo<T extends C & I> {
    
	/**
	 * @param foo
	 * @return
	 */
	public static <T extends C & I, U extends C & I> Foo<U> getX(Foo<T> foo) {
		return foo.getX();
	}

	<U extends C & I> Foo<U> getX() {
        return null;
    }

    Foo<?> f2 = Foo.getX(this);
}
