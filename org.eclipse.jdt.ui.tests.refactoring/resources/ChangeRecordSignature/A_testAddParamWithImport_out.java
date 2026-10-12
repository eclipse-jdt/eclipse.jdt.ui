package p;

import java.util.List;

record A(int a, String b, List<String> c){
}
class B {
	protected void m(){
		A a = new A(1, "string", null);
	}
}
