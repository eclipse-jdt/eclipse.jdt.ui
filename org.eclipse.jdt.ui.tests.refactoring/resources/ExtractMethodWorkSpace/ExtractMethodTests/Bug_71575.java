

import java.util.BitSet;

public class Bug_71575 {
	private int[] getIntArray() {
		return new int[0];
	}
	
	public void f1() {
		int[] intArray = getIntArray();
		
		BitSet bitSet1 = new BitSet();
		for (int i = 0 ; i < intArray.length ; i++) {
			bitSet1.set(intArray[i]);
		}
		
		BitSet bitSet2 = new BitSet();
		for (int i = 0 ; i < intArray.length ; i++) {
			bitSet2.set(intArray[i] + 1);
		}
		
		bitSet1.and(bitSet2);
	}

	public void f2() {
		int[] intArray = getIntArray();
		
		BitSet bitSet1 = new BitSet();
		for (int i = 0 ; i < intArray.length ; i++) {
			bitSet1.set(intArray[i]);
		}
		
		BitSet bitSet2 = new BitSet();
		for (int i = 0 ; i < intArray.length ; i++) {
			bitSet2.set(intArray[i] + 1);
		}

		bitSet1.and(bitSet2);	
	}	
}