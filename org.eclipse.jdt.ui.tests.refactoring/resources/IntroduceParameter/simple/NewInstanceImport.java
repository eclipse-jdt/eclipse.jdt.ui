//selection: 9, 20, 9, 46
//name: iterator -> iter
package simple;

import java.util.ArrayList;

public class NewInstanceImport {
	public void m(int a) {
		boolean b= new ArrayList().iterator().hasNext();
	}
	public void use() {
		m(17);
	}
}
