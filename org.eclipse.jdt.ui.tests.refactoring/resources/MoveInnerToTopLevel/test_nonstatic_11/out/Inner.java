package p;
class Inner{
	private A a;
	Inner(A a){
		this.a= a;
	}
	void f(){
		new Inner(this.a);
	}
}