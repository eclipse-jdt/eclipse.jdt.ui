package locals_in;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class A_test535 {

	public void bar() {
		List allElements= new ArrayList();
		Iterator iter= allElements.iterator();		

		/*[*/while (iter.hasNext()) {
			allElements.add(iter.next());
		}/*]*/
	}
}
