package p;

class A {
	void calc(Class superClass) {
		superClass= superClass.getSuperclass();
	}
	void call(Process pro) {
		calc(pro.getClass());
	}
}

class Exposer {
	private void foo() {
		new Generic(getClass());
		new Generic(Exposer.class);
	}
}

class Generic<T> {
	Generic(Class<T> clazz) {
		
	}
}
