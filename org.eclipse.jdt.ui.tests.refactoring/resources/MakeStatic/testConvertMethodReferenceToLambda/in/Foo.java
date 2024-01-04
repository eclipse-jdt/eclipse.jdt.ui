import java.util.function.Function;

class Foo {
	Foo2 method(Function<Foo, Foo2> function) {
		return function.apply(this);
	}
}

class Foo2 {
	Foo2 bar(Foo foo) {
		return this;
	}
}

class Foo3 {
	public void method2() {
		Foo2 bar= new Foo2();
		new Foo().method(bar::bar);
	}
}
