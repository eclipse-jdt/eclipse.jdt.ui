package p;

import java.util.ArrayList;
import java.util.List;

public class A {
	void m(){
		B b= new B();
		b.j= 0;
	}
}
class B extends A{
	public int j= 0;
	}
