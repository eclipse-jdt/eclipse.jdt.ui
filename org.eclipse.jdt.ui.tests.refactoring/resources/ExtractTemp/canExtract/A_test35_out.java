package p;

import java.util.List;

class A {
	void f(){
		C b= new C();
		List[] temp= b.lists();
		Object x= temp;
	}
} 
class C{
	public List[] lists(){
		return null;
	}
}
