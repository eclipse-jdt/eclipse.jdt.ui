package p;
//use B
interface I{}
class C implements I{
}

class B extends C{
}
class A extends B{
}

class Test{
	void f(){
		B c= new A();
		c.toString();
	}
}