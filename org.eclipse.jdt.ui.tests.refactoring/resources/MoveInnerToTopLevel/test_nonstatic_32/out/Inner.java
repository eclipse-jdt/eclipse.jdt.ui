package p;
class Inner{
	private final A a;
	Inner(A a) {
		this.a= a;
	}
    void f(){
        this.a.m= 1;
    }
}