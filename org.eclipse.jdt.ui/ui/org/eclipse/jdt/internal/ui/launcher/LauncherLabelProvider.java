/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

package org.eclipse.jdt.internal.ui.launcher;

import org.eclipse.jdt.ui.JavaElementLabelProvider;


/**
 * We have to subclass the label provider in order to have a default constructor.
 * This is necessary because we are instantiated as an extension point.
 */
public class LauncherLabelProvider extends JavaElementLabelProvider  {
	
	public LauncherLabelProvider() {
		super(JavaElementLabelProvider.SHOW_DEFAULT | JavaElementLabelProvider.SHOW_CONTAINER_QUALIFICATION | 
			JavaElementLabelProvider.SHOW_ROOT | JavaElementLabelProvider.SHOW_POSTIFIX_QUALIFICATION);
	}
}