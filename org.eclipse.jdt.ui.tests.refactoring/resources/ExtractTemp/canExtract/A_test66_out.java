package p; //7, 32 - 7, 33

class A {
	void f(String bar) {
		try {
		} catch (Exception e) {
			Exception temp= e;
			Exception another= temp;
		}
	}
}