package bugs_in;

public class Test_44419 {
	
	protected long fValue1= /*]*/getValue(1 + 3)/*[*/;
	
	long getValue(int i) {
		return System.currentTimeMillis() * i;
	}
}
