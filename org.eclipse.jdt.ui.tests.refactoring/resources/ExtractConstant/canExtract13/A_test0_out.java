//6, 19 -> 9, 22   AllowLoadtime == false
package p;
import java.lang.String;
class A {
	private static final String CONSTANT= """
			  abc
			  def
			  """;

	void f() {
		String i= CONSTANT;
	}
}