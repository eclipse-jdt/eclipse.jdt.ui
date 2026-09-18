package p;

import java.util.List;

record A(int a, List<String> b){
}
class B {
	protected void m(){
		A a = new A(1, null);
	}
}
