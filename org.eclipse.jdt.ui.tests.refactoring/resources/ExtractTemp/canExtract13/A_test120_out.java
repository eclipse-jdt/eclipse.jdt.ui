//6, 19 -> 9, 22   AllowLoadtime == false
package p;
import java.lang.String;
class A {
	void f() {
		String string= """
				  abc
				  def
				  """;
		String i= string;
	}
}
