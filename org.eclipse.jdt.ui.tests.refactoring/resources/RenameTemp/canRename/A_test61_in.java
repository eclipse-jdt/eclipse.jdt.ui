package p;

class A {
    void m() {
    		int abc = 0;
		if (true) {
			 System.out.println(/*[*/abc/*]*/) // syntax error
		}
    }
    void take(int abc) {
        int x= abc;
    }
}