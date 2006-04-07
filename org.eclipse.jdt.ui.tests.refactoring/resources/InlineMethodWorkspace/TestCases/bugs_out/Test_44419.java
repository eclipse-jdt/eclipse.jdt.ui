package bugs_out;

public class Test_44419 {
	
	protected long fValue1= /*]*/System.currentTimeMillis() * (1 + 3)/*[*/;
	
	long getValue(int i) {
		return System.currentTimeMillis() * i;
	}
}
