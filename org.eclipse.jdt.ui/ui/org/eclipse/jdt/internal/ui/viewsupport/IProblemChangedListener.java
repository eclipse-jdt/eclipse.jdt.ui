/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.viewsupport;

import java.util.Set;

/**
 * Can be added to a ProblemMarkerManager to get notified about error
 * marker changes. Used to update error ticks.
 */
public interface IProblemChangedListener {
	
	/**
	 * @param changedElements  A set of IPath that describe the resources
	 * the had a error marker change.
	 */
	void problemsChanged(Set changedElements);
}