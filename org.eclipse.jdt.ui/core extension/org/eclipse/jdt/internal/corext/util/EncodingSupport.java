/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0f
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.util;

import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.IEncodedStreamContentAccessor;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;


/**
 * EncodingSupport provides utility methods to determine and verify the encoding of
 * objects with streamable contents.
 * <p>
 * <b>Note</b>: This interface is part of an internal API that may well 
 * change in incompatible ways until it reaches its finished form. 
 * </p>
 */
public class EncodingSupport {

	public static String getCharset(IPath path) {
		return verifyCharset(null);
	}
	
	public static String getCharset(IStorage object) {
		String encoding= null;
		if (object instanceof IEncodedStorage) {
			try {
				encoding= ((IEncodedStorage)object).getCharset();
			} catch (CoreException e) {
			}
		}
		return verifyCharset(encoding);
	}
	
	public static String getCharset(IStreamContentAccessor object) {
		String encoding= null;
		if (object instanceof IEncodedStreamContentAccessor) {
			try {
				encoding= ((IEncodedStreamContentAccessor)object).getCharset();
			} catch (CoreException e) {
			}
		}
		return verifyCharset(encoding);
	}
	
	public static String verifyCharset(String encoding) {
		if (encoding == null)
			return ResourcesPlugin.getEncoding();
		return encoding;
	}
}
