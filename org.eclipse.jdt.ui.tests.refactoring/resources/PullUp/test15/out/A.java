package p;

import java.util.ArrayList;
import java.util.List;

public class A {
	protected void m(){
		B b= new B();
		b.f();
	}

}
class B extends A{
	public void f(){}
	}
