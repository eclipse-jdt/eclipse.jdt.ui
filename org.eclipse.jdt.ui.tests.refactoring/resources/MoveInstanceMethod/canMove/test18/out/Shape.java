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
		return fInnerBounds.filledArea(this);
	}
	
	public boolean isSmallerThan(Rectangle rect) {
		return area() < rect.getWidth() * rect.getHeight();
	}
	
}
