package p;
//use C
interface I{}
class C implements I{
}

class B extends C{
}
class A extends B{
}

class Test{
	void f(){
		C c= new A();
		c.toString();
	}
}