package p;
class C {
}

interface I {
}

public class Foo<T extends C & I> {
    
	<U extends C & I> Foo<U> getX() {
        return null;
    }

    Foo<?> f2 = getX();
}
