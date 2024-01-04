import java.util.function.Function;

class Foo {
	void method() {
		Foo2<Foo> function= Foo::bar;
	}

	void bar() {
	}
}

interface Foo2<T> {
	void bar(T j);
}
