package p;
class A{
	A aField;
}
class B extends A{
	void f(){
		A a= null;
		super.aField= a;
	}
}