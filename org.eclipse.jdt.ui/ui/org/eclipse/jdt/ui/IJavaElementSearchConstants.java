/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.ui;

/**
 * Search scope constants for Java selection dialogs.
 * <p>
 * This interface declares constants only; it is not intended to be implemented.
 * </p>
 *
 * @see JavaUI
 */
public interface IJavaElementSearchConstants {

	/** 
	 * Search scope constant (bit mask) indicating that classes should be considered.
	 * Used when opening certain kinds of selection dialogs.
	 */
	public static final int CONSIDER_CLASSES= 1 << 1;

	/** 
	 * Search scope constant (bit mask) indicating that interfaces should be considered.
	 * Used when opening certain kinds of selection dialogs.
	 */
	public static final int CONSIDER_INTERFACES= 1 << 2;

	/**
	 * Search scope constant (bit mask) indicating that both classes and interfaces 
	 * should be considered. Equivalent to
	 * <code>CONSIDER_CLASSES | CONSIDER_INTERFACES</code>.
	 */
	public static final int CONSIDER_TYPES= CONSIDER_CLASSES | CONSIDER_INTERFACES;

	/**
	 * Search scope constant (bit mask) indicating that binaries should be considered.
	 * Used when opening certain kinds of selection dialogs.
	 */
	public static final int CONSIDER_BINARIES= 1 << 3;

	/**
	 * Search scope constant (bit mask) indicating that external JARs should be considered.
	 * Used when opening certain kinds of selection dialogs.
	 */
	public static final int CONSIDER_EXTERNAL_JARS= 1 << 4;
}
