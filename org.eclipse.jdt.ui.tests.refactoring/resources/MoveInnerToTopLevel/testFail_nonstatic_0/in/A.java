package p;
class A{
	class Inner{
		int a; //conflicting name
		{foo();} //needs enclosing instance
	}
	void foo() {}
}