package p;

class A {
	void f(){
		for (int i= 0; i < 5; i++) {
			for (int k= 0, p= 17; p < i; k++) {
				String temp= i++ + " " + k;
				System.out.println(temp);
			}
		}
	}
}
