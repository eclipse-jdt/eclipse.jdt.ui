/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.wizards.swt;

/**
 * A utility class to create convenient grid data objects.
 * @private Should not be used outside JFace.
 */
public class MGridUtil {

	/**
	 * Creates a grid data object that occupies vertical and horizontal
	 * space.
	 */
	static public MGridData createFill() {
		MGridData gd= new MGridData();
		gd.horizontalAlignment= gd.FILL;
		gd.grabExcessHorizontalSpace= true;
		gd.verticalAlignment= gd.FILL;
		gd.grabExcessVerticalSpace= true;
		return gd;
	}
	
	/**
	 * Creates a grid data object that occupies horizontal space.
	 */
	static public MGridData createHorizontalFill() {
		MGridData gd= new MGridData();
		gd.horizontalAlignment= gd.FILL;
		gd.grabExcessHorizontalSpace= true;
		return gd;
	}

	/**
	 * Creates a grid data object that occupies vertical space.
	 */
	static public MGridData createVerticalFill() {
		MGridData gd= new MGridData();
		gd.verticalAlignment= gd.FILL;
		gd.grabExcessVerticalSpace= true;
		return gd;
	}		
}