package p; //6, 19 - 2, 22

class A {
	void f(String bar) {
		try {
		} catch (Exception e) {
			Exception temp= e;
			Exception another= temp;
		}
	}
}