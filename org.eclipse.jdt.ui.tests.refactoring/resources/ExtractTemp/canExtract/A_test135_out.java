package p; // 6, 18, 6, 31
class A {
	int[] a;

	void method(int index) {
		int f= this.f(index);
		int i1 = f;
		int i2 = this.f(index);
		return;
	}

	int f(int index) {
		return a[index] *= a[index];
	}
}