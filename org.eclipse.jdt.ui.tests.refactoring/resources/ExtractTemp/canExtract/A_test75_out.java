package p;

class A {
	void f(){
		for (int i= 0; i < 5; i++) {
			for (int k= 0, p= 17; p < i; k++) {
				int temp= i++;
				System.out.println(temp + " " + k);
			}
		}
	}
}
