/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.compare;

import org.eclipse.compare.ZipFileStructureCreator;

/**
 * A JarStructureCreator is the same as a ZipFileStructureCreator
 * but has a different name.
 */
public class JarStructureCreator extends ZipFileStructureCreator {

	public JarStructureCreator() {
		super(CompareMessages.getString("JarStructureCreator.name")); //$NON-NLS-1$
	}
}

