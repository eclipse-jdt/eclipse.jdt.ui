package p;

public class Shape {
	Rectangle fBounds;
	Rectangle fInnerBounds;
	
	public int area() {
		int width= fBounds.getWidth();
		int height= fBounds.getHeight();
		return width*height;
	}
	
	public int filledArea() {
		return area() -	fInnerBounds.getWidth() * fInnerBounds.getHeight();
	}

	/**
	 * Is smaller
	 * @param rect
	 * @return boolean
	 */
	public boolean isSmallerThan(Rectangle rect) {
		return rect.isSmallerThan(this);
	}
	
}
