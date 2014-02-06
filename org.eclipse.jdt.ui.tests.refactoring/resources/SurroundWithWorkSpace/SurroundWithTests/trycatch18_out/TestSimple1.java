package trycatch18_out;

import java.io.FileNotFoundException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

class TestSimple1 {
	void foo(int a) {
		try {
			/*[*/throw new @Marker FileNotFoundException();/*]*/
		} catch (@Marker FileNotFoundException e) {
		}
	}
}

@Target(ElementType.TYPE_USE)
@interface Marker { }