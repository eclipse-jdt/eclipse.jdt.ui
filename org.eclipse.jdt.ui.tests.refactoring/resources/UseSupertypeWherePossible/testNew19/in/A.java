package p;
//use I
interface I{}
class C implements I{
}

class B extends C{
}
class A extends B{
}

class Test{
	void f(){
		A c= new A();
		c.toString();
	}
}