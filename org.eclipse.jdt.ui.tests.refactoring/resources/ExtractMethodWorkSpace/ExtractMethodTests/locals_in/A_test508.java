package locals_in;
import java.util.ArrayList;import java.util.Iterator;import java.util.List;public class A_test508 {

	public void bar() {
		Iterator iter= null;
		
		List allElements= new ArrayList();

		/*]*/while (iter.hasNext()) {
			allElements.add(iter.next());
		}/*[*/
	}
}
