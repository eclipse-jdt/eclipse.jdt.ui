package p;

import javax.swing.JTextField;

class A extends JTextField implements I{
}
class Test{
	void f(A a){
		x(a);
	}
	void x(JTextField o){}
}