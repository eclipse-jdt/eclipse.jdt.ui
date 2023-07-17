/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Microsoft Corporation - copied to jdt.core.manipulation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.refactoring.nls.changes;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.core.resources.IFile;

import org.eclipse.jdt.core.IJavaModelStatusConstants;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.corext.refactoring.nls.NLSUtil;

public class CreateTextFileChange extends CreateFileChange {

	private final String fTextType;

	public CreateTextFileChange(IPath path, String source, String encoding, String textType) {
		super(path, source, encoding);
		fTextType= textType;
	}

	public String getTextType() {
		return fTextType;
	}

	public String getCurrentContent() throws JavaModelException {
		IFile file= getOldFile(new NullProgressMonitor());
		if (!file.exists())
			return ""; //$NON-NLS-1$
		try (InputStream stream= file.getContents()) {
			String encoding= file.getCharset();
			String c= NLSUtil.readString(stream, encoding);
			return (c == null) ? "" : c; //$NON-NLS-1$
		} catch (IOException | CoreException e) {
			throw new JavaModelException(e, IJavaModelStatusConstants.CORE_EXCEPTION);
		}
	}

	public String getPreview() {
		return getSource();
	}
}

