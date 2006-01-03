package p;

class A {
	/**
	 * @deprecated Use {@link #m(int[],int)} instead
	 */
	void m(int a, int b[]){
		m(b, a);
	}

	void m(int b[], int a){}
}