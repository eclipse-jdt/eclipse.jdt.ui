package p;

import java.util.ArrayList;

class A extends ArrayList{
	public static final int X= 0;
}
class Test{
	void f(A a){
		x(a);
	}
	void x(ArrayList o){}
}