package p;

import java.util.List;

class A {
	void f(){
		C b= new C();
		Object x= b.lists();
	}
} 
class C{
	public List[] lists(){
		return null;
	}
}
