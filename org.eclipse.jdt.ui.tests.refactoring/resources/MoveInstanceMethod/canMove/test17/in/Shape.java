package p;

public class Shape {
	Rectangle fBounds;
	Rectangle fInnerBounds;

	/**
	 * Returns the area
	 * @return the area
	 */
	public int area() {
		int width= fBounds.getWidth();
		int height= fBounds.getHeight();
		return width*height;
	}
	
	public int filledArea() {
		return area() -	fInnerBounds.getWidth() * fInnerBounds.getHeight();
	}
	
	public boolean isSmallerThan(Rectangle rect) {
		return area() < rect.getWidth() * rect.getHeight();
	}
	
}
