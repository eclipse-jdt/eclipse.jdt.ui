/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.refactoring;

import org.eclipse.jdt.internal.corext.refactoring.base.IChange;

public interface IChangeElementChildrenCreator {

	public ChangeElement[] create(ChangeElement parent, IChange change);
}

