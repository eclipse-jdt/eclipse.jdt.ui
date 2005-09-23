package p5;
class MoreInner {

	/** Comment */
	private final A.Inner inner;

	/**
	 * @param inner
	 */
	MoreInner(A.Inner inner) {
		this.inner= inner;
	}

	{
		this.inner.someField++;
	}

}