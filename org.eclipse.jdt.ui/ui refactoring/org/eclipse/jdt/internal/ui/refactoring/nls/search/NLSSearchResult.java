/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.ui.refactoring.nls.search;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;

import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.IEditorMatchAdapter;
import org.eclipse.search.ui.text.IFileMatchAdapter;
import org.eclipse.search.ui.text.Match;

import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.internal.ui.javaeditor.IClassFileEditorInput;

public class NLSSearchResult extends AbstractTextSearchResult implements IEditorMatchAdapter, IFileMatchAdapter {
	/*
	 * Element (group key) is always IJavaElement or FileEntry.
	 */
	
	private NLSSearchQuery fQuery;
	private FileEntry fDuplicatesGroup;
	private FileEntry fUnusedGroup;

	public NLSSearchResult(NLSSearchQuery query) {
		fQuery= query;
	}
	
	public void setDuplicatesGroup(FileEntry duplicatesGroup) {
		fDuplicatesGroup= duplicatesGroup;
	}
	
	public void setUnusedGroup(FileEntry unusedGroup) {
		fUnusedGroup= unusedGroup;
	}
	
	/*
	 * @see org.eclipse.search.ui.text.AbstractTextSearchResult#findContainedMatches(org.eclipse.ui.IEditorPart)
	 */
	public Match[] computeContainedMatches(AbstractTextSearchResult result, IEditorPart editor) {
		//TODO: copied from JavaSearchResult:
		IEditorInput editorInput= editor.getEditorInput();
		if (editorInput instanceof IFileEditorInput)  {
			IFileEditorInput fileEditorInput= (IFileEditorInput) editorInput;
			return computeContainedMatches(result, fileEditorInput.getFile());
		} else if (editorInput instanceof IClassFileEditorInput) {
			IClassFileEditorInput classFileEditorInput= (IClassFileEditorInput) editorInput;
			Set matches= new HashSet();
			collectMatches(matches, classFileEditorInput.getClassFile());
			return (Match[]) matches.toArray(new Match[matches.size()]);
		}
		return null;
	}

	/*
	 * @see org.eclipse.search.ui.text.AbstractTextSearchResult#findContainedMatches(org.eclipse.core.resources.IFile)
	 */
	public Match[] computeContainedMatches(AbstractTextSearchResult result, IFile file) {
		if (fQuery.getPropertiesFile().equals(file)) {
			ArrayList matches= new ArrayList();
			if (fDuplicatesGroup != null)
				matches.addAll(Arrays.asList(getMatches(fDuplicatesGroup)));
			if (fUnusedGroup != null)
				matches.addAll(Arrays.asList(getMatches(fUnusedGroup)));
			return (Match[]) matches.toArray(new Match[matches.size()]);
		} else {
			//TODO: copied from JavaSearchResult:
			IJavaElement javaElement= JavaCore.create(file);
			Set matches= new HashSet();
			collectMatches(matches, javaElement);
			return (Match[]) matches.toArray(new Match[matches.size()]);
		}
	}
	
	private void collectMatches(Set matches, IJavaElement element) {
		//TODO: copied from JavaSearchResult:
		Match[] m= getMatches(element);
		if (m.length != 0) {
			for (int i= 0; i < m.length; i++) {
				matches.add(m[i]);
			}
		}
		if (element instanceof IParent) {
			IParent parent= (IParent) element;
			try {
				IJavaElement[] children= parent.getChildren();
				for (int i= 0; i < children.length; i++) {
					collectMatches(matches, children[i]);
				}
			} catch (JavaModelException e) {
				// we will not be tracking these results
			}
		}
	}
	
	/*
	 * @see org.eclipse.search.ui.text.AbstractTextSearchResult#getFile(java.lang.Object)
	 */
	public IFile getFile(Object element) {
		if (element instanceof FileEntry) {
			return ((FileEntry) element).getPropertiesFile();
		} else {
			IJavaElement javaElement= (IJavaElement) element;
			IResource resource= null;
			try {
				resource= javaElement.getCorrespondingResource();
			} catch (JavaModelException e) {
				// no resource
			}
			if (resource instanceof IFile)
				return (IFile) resource;
			else
				return null;
		}
	}

	/*
	 * @see org.eclipse.search.ui.text.AbstractTextSearchResult#isShownInEditor(org.eclipse.search.ui.text.Match, org.eclipse.ui.IEditorPart)
	 */
	public boolean isShownInEditor(Match match, IEditorPart editor) {
		IEditorInput editorInput= editor.getEditorInput();
		if (match.getElement() instanceof FileEntry) {
			IFile file= ((FileEntry) match.getElement()).getPropertiesFile();
			if (editorInput instanceof IFileEditorInput) {
				return ((IFileEditorInput) editorInput).getFile().equals(file);
			}
		} else if (match.getElement() instanceof IJavaElement) {
			IJavaElement je= (IJavaElement) match.getElement();
			if (editorInput instanceof IFileEditorInput) {
				try {
					ICompilationUnit cu= (ICompilationUnit) je.getAncestor(IJavaElement.COMPILATION_UNIT);
					if (cu == null)
						return false;
					else
						return ((IFileEditorInput) editorInput).getFile().equals(cu.getCorrespondingResource());
				} catch (JavaModelException e) {
					return false;
				}
			} else if (editorInput instanceof IClassFileEditorInput) {
				return ((IClassFileEditorInput) editorInput).getClassFile().equals(je.getAncestor(IJavaElement.CLASS_FILE));
			}
		}
		return false;
	}

	/*
	 * @see org.eclipse.search.ui.ISearchResult#getLabel()
	 */
	public String getLabel() {
		int matchCount= getMatchCount();
		if (matchCount == 1) {
			return fQuery.getSingularLabel();
		} else {
			String format= fQuery.getPluralLabelPattern();
			return MessageFormat.format(format, new Object[] { new Integer(matchCount) });
		}
	}

	/*
	 * @see org.eclipse.search.ui.ISearchResult#getTooltip()
	 */
	public String getTooltip() {
		return getLabel();
	}

	/*
	 * @see org.eclipse.search.ui.ISearchResult#getImageDescriptor()
	 */
	public ImageDescriptor getImageDescriptor() {
		return JavaPluginImages.DESC_OBJS_SEARCH_REF;
	}

	/*
	 * @see org.eclipse.search.ui.ISearchResult#getQuery()
	 */
	public ISearchQuery getQuery() {
		return fQuery;
	}

	public IFileMatchAdapter getFileMatchAdapter() {
		return this;
	}
	
	public IEditorMatchAdapter getEditorMatchAdapter() {
		return this;
	}

}
