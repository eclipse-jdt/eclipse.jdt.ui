package p;

public class A {
	
	void bar() {
		B site = null;
		baz(site.f, site.f);
	}

	void baz(C filters, C fs) {
		filters.foo();
	}
	
}
