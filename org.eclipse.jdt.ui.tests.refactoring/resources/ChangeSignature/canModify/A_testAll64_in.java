package p;

class A {
	void m() {
		new Runnable() {
			public void run() {
				new Cloneable() {
					public Object clone() {
						nasty("a", new Local());
						return null;
					}
					class Local {}
					private void nasty(String name, Local i) {
						
					}
				};
			}
		};
	}
}