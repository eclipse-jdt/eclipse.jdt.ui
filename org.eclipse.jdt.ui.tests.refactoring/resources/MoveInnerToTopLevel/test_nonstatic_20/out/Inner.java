package p;
class Inner{
	/** Comment */
	private A a;
	Inner(A a){
		super();
		this.a= a;
	}
	Inner(A a, int i){
		this(a);
	}
	Inner(A a, boolean b){
		this(a, 1);
	}
}