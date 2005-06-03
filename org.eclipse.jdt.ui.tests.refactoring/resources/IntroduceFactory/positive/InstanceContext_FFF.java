package p;

public class InstanceContext_in {
	public static InstanceContext_in createInstanceContext_in(int N) {
		return new InstanceContext_in(N);
	}
	private int fN;
	public InstanceContext_in(int N) {
		fN= N;
	}
	public int get() {
		return fN;
	}
	public void foo(String[] args) {
		InstanceContext_in	ic= createInstanceContext_in(16);

		System.out.println("Value = " + ic.get());
	}
}
