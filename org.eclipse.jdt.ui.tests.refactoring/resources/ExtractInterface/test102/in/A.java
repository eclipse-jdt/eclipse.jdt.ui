package p;

public class A<T> {
	
	void bar() {
		B site = null;
		baz(site.f, site.f);
	}

	void baz(C filters, C fs) {
		filters.foo();
	}

	T wassup() {
		return null;
	}
}
