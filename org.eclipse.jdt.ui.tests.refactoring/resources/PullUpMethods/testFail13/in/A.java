package p;

import java.util.ArrayList;
import java.util.List;

public class A {
}
class B extends A{
	public void f(){}
	void m(){
		f();
	}
}
