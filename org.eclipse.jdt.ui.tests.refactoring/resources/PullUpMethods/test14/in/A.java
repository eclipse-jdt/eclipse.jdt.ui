package p;

import java.util.ArrayList;
import java.util.List;

public class A {
void g(){
	g();
	}
}
class B extends A{
	protected void m(){
		List l= new ArrayList();
		l.size();
	}	
}
