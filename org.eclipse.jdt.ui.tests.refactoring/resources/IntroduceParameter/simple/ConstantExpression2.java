//selection: 12, 16, 12, 52
//name: i -> length
package simple;

import java.util.Vector;

public class ConstantExpression2 {
	private Vector fBeginners;
	private Vector fAdvanced;
	
	private int count() {
		return fBeginners.size() + fAdvanced.size();
	}
	public void use() {
		count();
	}
}
