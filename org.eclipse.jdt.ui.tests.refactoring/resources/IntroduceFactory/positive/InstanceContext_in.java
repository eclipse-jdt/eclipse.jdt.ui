package p;

public class InstanceContext_in {
	private int fN;
	public InstanceContext_in(int N) {
		fN= N;
	}
	public int get() {
		return fN;
	}
	public void foo(String[] args) {
		InstanceContext_in	ic= /*[*/new InstanceContext_in(16)/*]*/;

		System.out.println("Value = " + ic.get());
	}
}
