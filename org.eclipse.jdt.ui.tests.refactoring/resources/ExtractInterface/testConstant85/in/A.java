package p;

import javax.swing.JTextField;

class A extends JTextField{
	public static final int X= 0;
}
class Test{
	void f(A a){
		x(a);
	}
	void x(JTextField o){}
}