package p;
class A{
	void f(boolean flag){
		if (flag){
			f(3+5==8); 
		} else 
			f(3+5!=8); 
	}
}