package locals_out;
import java.util.ArrayList;import java.util.Iterator;import java.util.List;public class A_test508 {

	public void bar() {
		Iterator iter= null;
		
		List allElements= new ArrayList();

		/*]*/extracted(iter, allElements);/*[*/
	}
	protected void extracted(Iterator iter, List allElements) {
		while (iter.hasNext()) {
			allElements.add(iter.next());
		}
	}
}
