//selection: 8, 20, 8, 46
package simple.out;

import java.util.ArrayList;
import java.util.Iterator;

public class NewInstanceImport {
	public void m(int a, Iterator iter) {
		boolean b= iter.hasNext();
	}
	public void use() {
		m(17, new ArrayList().iterator());
	}
}
