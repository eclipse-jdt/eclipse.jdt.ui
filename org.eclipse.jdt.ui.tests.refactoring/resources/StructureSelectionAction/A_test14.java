package p;
class A{
	void m(){
		int i= 1 + 1;
		if (i < 0){
			i += i;
			return;
		} else
		 m();
	}
}