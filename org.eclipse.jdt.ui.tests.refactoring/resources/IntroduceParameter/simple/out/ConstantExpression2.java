//selection: 12, 16, 12, 52
//name: i -> length
package simple.out;

import java.util.Vector;

public class ConstantExpression2 {
	private Vector fBeginners;
	private Vector fAdvanced;
	
	private int count(int length) {
		return length;
	}
	public void use() {
		count(fBeginners.size() + fAdvanced.size());
	}
}
