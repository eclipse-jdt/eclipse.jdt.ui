package p;

public class MultipleCallers_in {
	private int fN;
	public MultipleCallers_in(int N) {
		fN= N;
	}
	public int get() {
		return fN;
	}
	public void foo(String[] args) {
		MultipleCallers_in	mc= /*[*/new MultipleCallers_in(16)/*]*/;

		System.out.println("Value = " + mc.get());
	}
	public void bar(String[] args) {
		MultipleCallers_in	mc= new MultipleCallers_in(16);

		System.out.println("Value = " + mc.get());
	}
}
