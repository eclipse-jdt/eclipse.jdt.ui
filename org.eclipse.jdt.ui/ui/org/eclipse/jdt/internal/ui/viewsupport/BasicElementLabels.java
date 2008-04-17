/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.viewsupport;

import org.eclipse.core.runtime.IPath;

import org.eclipse.osgi.util.TextProcessor;

import org.eclipse.jdt.internal.ui.JavaPlugin;

public class BasicElementLabels {
	
	public static String getPathLabel(IPath path, boolean isOSPath) {
		String label;
		if (isOSPath) {
			label= path.toOSString();
		} else {
			label= path.makeRelative().toString();
		}
		if (JavaPlugin.USE_TEXT_PROCESSOR) {
			return TextProcessor.process(label);
		}
		return label;
	}
	
	public static String getFilePattern(String name) {
		if (JavaPlugin.USE_TEXT_PROCESSOR) {
			return TextProcessor.process(name);
		}
		return name;
	}	
}
