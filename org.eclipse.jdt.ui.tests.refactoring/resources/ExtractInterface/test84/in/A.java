package p;
class A{
	int x;
}
class B {
	A f(){
		A a= null;
		return (a);	
	}
	void x(){
		f().x= 0;
	}
}