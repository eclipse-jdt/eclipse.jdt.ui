package q;

public enum State {
	ONE, TWO, THREE;
	public static final State START= getStart();
	public static State getStart() {
		return ONE;
	}
}