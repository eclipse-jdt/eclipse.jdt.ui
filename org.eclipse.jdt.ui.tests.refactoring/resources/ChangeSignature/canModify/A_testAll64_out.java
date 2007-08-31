package p;

import java.util.List;

class A {
	void m() {
		new Runnable() {
			public void run() {
				new Cloneable() {
					public Object clone() {
						nasty(new Local(), null);
						return null;
					}
					class Local {}
					private void nasty(Local i, List<Local> list) {
						
					}
				};
			}
		};
	}
}