package p;

class A implements Bag {
	int notExtracted= 0;
	private A v2;
	void f(A v3){
		this.v2= v3;
		this.v2.notExtracted= 1;
	}
}