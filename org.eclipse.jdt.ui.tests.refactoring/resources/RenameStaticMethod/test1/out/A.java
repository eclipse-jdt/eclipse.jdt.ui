package p;
class A{
	static void k(){
	}
	void f(){
		k();
	}
	static int fred(){
		k();
	}
}
class B{
	static void m(){
		A.k();
		new A().k();
	}
}