package p;
class Inner{
	private A a;
	Inner(A a){
		this(a, 0);
	}
	Inner(A a, int i){
		this.a= a;
	}
}