package locals_out;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class A_test535 {

	public void bar() {
		List allElements= new ArrayList();
		Iterator iter= allElements.iterator();		

		extracted(allElements, iter);
	}

	protected void extracted(List allElements, Iterator iter) {
		/*[*/while (iter.hasNext()) {
			allElements.add(iter.next());
		}/*]*/
	}
}
