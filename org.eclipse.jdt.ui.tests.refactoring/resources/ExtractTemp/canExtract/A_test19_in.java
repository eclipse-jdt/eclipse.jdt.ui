package p;
class A{
	void m(int i){
		if (f() == 0){
			int t= f();
		}
	}
	int f(){
		return 5;
	}
}