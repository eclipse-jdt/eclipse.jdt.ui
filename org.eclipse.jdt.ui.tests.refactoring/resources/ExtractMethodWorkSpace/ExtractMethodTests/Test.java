import java.util.ArrayList;import java.util.Iterator;import java.util.List;public class Test {

	public void foo() {
		int i= 10;
		if (i < 10) {
			foo();
		}
		boolean b;
		if ((b= true))
			foo();
			
		// b= !b;	
	}
	
	public void g() {
		foo();
		foo();
	}
}
