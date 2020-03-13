package p;
record A(int g){
	A{
		this.g= g;
	}
	
	public void val(int f) {
		
	}

	public int getVal() {
		return g();
	}
	
	public int g() {
		return g;
	}
}