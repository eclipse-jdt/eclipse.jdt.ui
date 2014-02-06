package trycatch18_in;

import java.io.FileNotFoundException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

class TestSimple1 {
	void foo(int a) {
		/*[*/throw new @Marker FileNotFoundException();/*]*/
	}
}

@Target(ElementType.TYPE_USE)
@interface Marker { }