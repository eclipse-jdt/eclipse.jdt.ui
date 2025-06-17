package p;

class C<T> {
	void f(T t1) {
	}

	void g() {
		C.this.f(null);
	}
}