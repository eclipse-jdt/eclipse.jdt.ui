package trycatch18_in;

import java.net.Socket;

class TestSimple1 {
	void foo(int a) {
		/*[*/String s=new String("abc");
		s=s.concat("def");/*]*/
	}
}
