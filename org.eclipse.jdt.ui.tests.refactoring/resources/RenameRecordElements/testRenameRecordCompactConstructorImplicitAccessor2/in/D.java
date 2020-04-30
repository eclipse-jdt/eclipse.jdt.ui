package p;
record D(int f){
	A{
		this.f= f;
	}
	
	public void val(int f) {
		
	}

	public int getVal() {
		return f();
	}
	
	public static void val() {
		D d= new D(20);
		return d.f();
	}
}
