package p;
//can't init in constructor - name clash
class A{
	A(int i){
	}
	void f(){
		int i= 0;
	}
}