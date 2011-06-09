//selection: 12, 13, 12, 15
//name: fileNotFoundException -> first
package simple.out;

import java.io.FileNotFoundException;

public class Catch1 {
	public void foo(FileNotFoundException first) {
		try {
			throw new FileNotFoundException();
		} catch (FileNotFoundException ex) {
			first.printStackTrace();
		}
	}
}
