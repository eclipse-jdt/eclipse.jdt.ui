/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
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
