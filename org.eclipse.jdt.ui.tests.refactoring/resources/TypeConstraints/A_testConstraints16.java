package p;
class A{
	A aField= this;
	{
		A aTemp= this;
	}
	void f(){
		A a= this;
	}
}