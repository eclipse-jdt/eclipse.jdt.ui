//selection: 12, 13, 12, 15
//name: fileNotFoundException -> first
package simple;

import java.io.FileNotFoundException;

public class Catch1 {
	public void foo() {
		try {
			throw new FileNotFoundException();
		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
		}
	}
}
