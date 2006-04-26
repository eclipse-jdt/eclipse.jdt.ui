package p;
class A {
    B fB;
    
    public void doit(String doitArg) {
        subroutine(1.2f);
    }
    
    public void subroutine(float subArg) {
		fB.subroutine(this, subArg);
	}

    public void subsub() {
        
    }
    
}