package p;

public enum A {
	TEST;
	void bar() {
		B site = null;
		baz(site.f, site.f);
	}

	void baz(C filters, I fs) {
		filters.foo();
	}
	
}
