package p;

class A{
	class Inner{
		Inner(){
			super();
		}
		Inner(int i){
			this();
		}
		Inner(boolean b){
			this(1);
		}
	}
}