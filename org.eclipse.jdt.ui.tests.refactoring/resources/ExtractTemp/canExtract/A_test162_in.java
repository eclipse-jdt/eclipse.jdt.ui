package p; // 9, 13, 9, 17

import java.util.ArrayList;

public class A {
	private ArrayList list = null;

	public void updateBug() {
		if (list == null) {
			list = new ArrayList<>();
		}
		int index = 0;
		while (index < list.size()) {
			Object it = list.get(index++);
		}
	}
}