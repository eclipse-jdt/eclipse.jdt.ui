package p;

class A {
    void m() {
    		int x = 0;
		if (true) {
			 System.out.println(/*[*/x/*]*/) // syntax error
		}
    }
    void take(int abc) {
        int x= abc;
    }
}