class A {
	private boolean flag;
	protected void foo() {
		int i= 0;
		int j= 1;
		switch(j) {
			case 1:
				i= 10;
				i= 20;
				break;
			default:
				read(i);
		}
		read(i);
	}
	private void read(int i) {
	}
}