/**********************************************************************
Copyright (c) 2000, 2002 IBM Corp. and others.
All rights reserved. This program and the accompanying materials
are made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html

Contributors:
	IBM Corporation - Initial implementation
**********************************************************************/

package org.eclipse.jdt.ui.tests.packageview;

import org.eclipse.core.resources.IResourceDelta;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;

/**
 * Helper to test Java element delta handling.
 *
 */
class TestDelta implements IJavaElementDelta {

	private int fKind;
	private IJavaElement fElement;

	private IJavaElementDelta[] fAffectedChildren;

	public TestDelta(int kind, IJavaElement element) {
		fKind= kind;
		fElement= element;
	}

	public IJavaElementDelta[] getAddedChildren() {
		return null;
	}

	public IJavaElementDelta[] getAffectedChildren() {
		if (fAffectedChildren == null)
			return new IJavaElementDelta[0];
		else
			return fAffectedChildren;
	}

	public IJavaElementDelta[] getChangedChildren() {
		return null;
	}

	public IJavaElement getElement() {
		return fElement;
	}

	public int getFlags() {
		return 0;
	}

	public int getKind() {
		return fKind;
	}

	public IJavaElement getMovedFromElement() {
		return null;
	}

	public IJavaElement getMovedToElement() {
		return null;
	}

	public IJavaElementDelta[] getRemovedChildren() {
		return null;
	}

	public IResourceDelta[] getResourceDeltas() {
		return null;
	}

	public void setAffectedChildren(IJavaElementDelta[] children) {
		fAffectedChildren= children;
	}
	
	public static TestDelta createParentDeltas(IPackageFragment frag, TestDelta delta) {
		IJavaElement root= frag.getParent();
		TestDelta rootDelta= new TestDelta(IJavaElementDelta.CHANGED, root);

		IJavaProject proj= root.getJavaProject();
		TestDelta projectDelta= new TestDelta(IJavaElementDelta.CHANGED, proj);

		IJavaModel model= proj.getJavaModel();
		TestDelta modelDelta= new TestDelta(IJavaElementDelta.CHANGED, model);

		//set affected children
		modelDelta.setAffectedChildren(new IJavaElementDelta[] { projectDelta });
		projectDelta.setAffectedChildren(new IJavaElementDelta[] { rootDelta });
		rootDelta.setAffectedChildren(new IJavaElementDelta[] { delta });
		return modelDelta;
	}

	public static IJavaElementDelta createCUDelta(ICompilationUnit[] cu, IPackageFragment parent, int action) {
		TestDelta fragmentDelta= new TestDelta(IJavaElementDelta.CHANGED, parent);

		TestDelta[] deltas= new TestDelta[cu.length];
		for (int i= 0; i < cu.length; i++) {
			deltas[i]= new TestDelta(action, cu[i]);
		}

		fragmentDelta.setAffectedChildren(deltas);
		return createParentDeltas(parent, fragmentDelta);
	}

	public static IJavaElementDelta createDelta(IPackageFragment frag, int action) {
		TestDelta delta= new TestDelta(action, frag);
		return createParentDeltas(frag, delta);
	}
}
