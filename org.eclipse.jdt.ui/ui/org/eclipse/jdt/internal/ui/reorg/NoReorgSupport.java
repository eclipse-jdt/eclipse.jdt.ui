/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */

package org.eclipse.jdt.internal.ui.reorg;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.JavaModelException;

public class NoReorgSupport implements ICopySupport, IMoveSupport, IDeleteSupport, INamingPolicy {
	private static NoReorgSupport fInstance= new NoReorgSupport();

	public Object copyTo(Object element, Object p0, String newName, IProgressMonitor pm) {
		return null;
	}

	public boolean canCopy(List elements, Object p0) {
		return false;
	}

	public boolean isCopyable(Object element) {
		return false;
	}

	public boolean isCopyCompatible(List p0) {
		return false;
	}

	public boolean canBeAncestor(Object p0) {
		return false;
	}
	
	public static NoReorgSupport getInstance() {
		return fInstance;
	}

	public String isValidNewName(Object original, Object container, String name) {
		return null;
	}

	public Object moveTo(Object p0, Object p1, String p2, IProgressMonitor p3) throws JavaModelException, CoreException {
		return null;
	}

	public boolean canMove(List elements, Object dest) {
		return false;
	}
	
	public boolean isMovable(Object p0) {
		return false;
	}

	public boolean isMoveCompatible(List p0) {
		return false;
	}

	public boolean canReplace(Object p0, Object p1, String p2) {
		return false;
	}

	public void delete(Object p0, IProgressMonitor p1) throws JavaModelException, CoreException {
	}

	public boolean canDelete(Object p0) {
		return false;
	}
	
	public Object getElement(Object parent, String name) {
		return null;
	}

	public int getPathLength(Object p0) {
		return 0;
	}
	
	public String getElementName(Object element) {
		return element.toString();
	}
}