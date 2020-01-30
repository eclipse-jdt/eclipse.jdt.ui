/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.jdt.internal.ui.wizards.buildpaths;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.IJavaProject;

/**
 * RootCPListElement is special type of CPListElement whose children are CPListElement. They
 * represent the root node in the java build path tab and it may be possible to add or remove CPList
 * element from their children. They don't have any classpath entry corresponding to it
 *
 * @author Vikas Chandra
 */
public class RootCPListElement extends CPListElement {

	enum RootNodeChange {
		ToModulepath, ToClasspath, NoChange;

		static RootNodeChange fromOldAndNew(boolean wasModular, boolean isModular) {
			if (wasModular == isModular) {
				return NoChange;
			} else if (isModular) {
				return ToModulepath;
			} else {
				return ToClasspath;
			}
		}
	}

	private String fPathRootNodeName= null;

	private boolean fIsModuleRootNode;

	public RootCPListElement(IJavaProject project, int entryKind) {
		super(project, entryKind);
	}

	public RootCPListElement(IJavaProject project, String name, boolean isModuleRoot) {
		this(project, -1);
		fPathRootNodeName= name;
		fIsModuleRootNode= isModuleRoot;
	}

	@Override
	public boolean equals(Object other) {
		if (other != null && other.getClass().equals(getClass())) {
			// for root node, it should be exactly same object
			return this == other;
		}
		return false;
	}

	void addCPListElement(CPListElement cpe) {
		fChildren.add(cpe);
	}

	void addCPListElement(List<CPListElement> elementsToAdd) {
		fChildren.addAll(elementsToAdd);
	}

	ArrayList<Object> getChildren() {
		return fChildren;
	}

	String getPathRootNodeName() {
		return fPathRootNodeName;
	}

	@Override
	boolean isRootNodeForPath() {
		return true;
	}

	@Override
	boolean isModulePathRootNode() {
		return fIsModuleRootNode;
	}

	@Override
	boolean isClassPathRootNode() {
		return !fIsModuleRootNode;
	}

	public boolean isSourceRootNode(RootNodeChange changeNodeDirection) {
		return RootNodeChange.fromOldAndNew(fIsModuleRootNode, !fIsModuleRootNode) == changeNodeDirection;
	}

	public boolean isTargetRootNode(RootNodeChange changeNodeDirection) {
		return RootNodeChange.fromOldAndNew(!fIsModuleRootNode, fIsModuleRootNode) == changeNodeDirection;
	}

	void removeCPListElement(CPListElement element) {
		fChildren.remove(element);
	}
}
