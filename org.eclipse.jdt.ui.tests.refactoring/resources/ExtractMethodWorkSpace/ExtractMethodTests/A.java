class A {
	void f() {
		int i= 0;
		int j= 1;
		switch (j) {
			case 1 :
				/*[*/
				i= 1; /*]*/
				break;
			default :
				i= -1;
				break;
		}
		System.out.println(i);
	}
}