package p;
record A(int f){
	A (int f){
		int g= 0;
		this.f= f;
	}
	
	public void val(int f) {
		
	}

	public int getVal() {
		return f();
	}
	
	public int f() {
		return f;
	}
	
	public int g() {
		return f;
	}
}