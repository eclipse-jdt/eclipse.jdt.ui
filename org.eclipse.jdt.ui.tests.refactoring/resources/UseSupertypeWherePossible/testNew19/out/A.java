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
		I c= new A();
		c.toString();
	}
}