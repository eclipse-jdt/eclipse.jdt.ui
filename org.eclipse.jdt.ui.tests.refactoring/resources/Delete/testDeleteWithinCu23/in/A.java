package p;
class A{
	private int nestingDepth;
	private boolean openOnRun = true;
	public boolean getOpenOnRun() {
		return openOnRun;
	}
	protected int getNestingDepth() {
		return nestingDepth;
	}
}