package p; // 9, 13, 9, 17

import java.util.ArrayList;

public class A {
	private ArrayList list = null;

	public void updateBug() {
		ArrayList list2= list;
		if (list2 == null) {
			list2= list = new ArrayList<>();
		}
		int index = 0;
		while (index < list2.size()) {
			Object it = list2.get(index++);
		}
	}
}