package p;
class A{
	void f(boolean flag){
		int temp= 3+5;
		if (flag){
			f(temp==8); 
		} else 
			f(temp!=8); 
	}
}