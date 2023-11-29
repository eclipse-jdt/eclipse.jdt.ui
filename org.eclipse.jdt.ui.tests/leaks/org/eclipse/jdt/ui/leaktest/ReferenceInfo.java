/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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

package org.eclipse.jdt.ui.leaktest;

import java.util.ArrayList;

import org.eclipse.jdt.ui.leaktest.reftracker.ReferencedArrayElement;
import org.eclipse.jdt.ui.leaktest.reftracker.ReferencedFieldElement;
import org.eclipse.jdt.ui.leaktest.reftracker.ReferencedObject;

/**
 * Result element with information to the link to the root element
 */

public class ReferenceInfo {

	private final Object fReference;
	private final BacklinkNode[] fBacklinkNodes;
	private boolean fIsPosibleLeak;

	public ReferenceInfo(ReferencedObject ref) {
		fReference= ref.getValue();
		fBacklinkNodes= getBacklinkNodes(ref);
	}

	public boolean isPosibleLeak() {
		return fIsPosibleLeak;
	}

	public void setPosibleLeak(boolean isPosibleLeak) {
		fIsPosibleLeak= isPosibleLeak;
	}

	private BacklinkNode[] getBacklinkNodes(ReferencedObject curr) {
		ArrayList<BacklinkNode> res= new ArrayList<>();

		while (curr != null) {
			String str;
			if (curr instanceof ReferencedArrayElement) {
				ReferencedArrayElement ref= (ReferencedArrayElement) curr;
				String name= ref.getReferenceHolder().getValue().getClass().getComponentType().getName();
				str= name + String.valueOf('[') + ref.getIndex() + String.valueOf(']');
			} else if (curr instanceof ReferencedFieldElement) {
				ReferencedFieldElement ref= (ReferencedFieldElement) curr;
				String name= ref.getField().getDeclaringClass().getName();
				str= name + "#" + ref.getField().getName();
			} else {
				str= curr.getValue().getClass().getName();
			}
			res.add(new BacklinkNode(str, curr.getValue()));
			curr= curr.getReferenceHolder();
		}
		return res.toArray(new BacklinkNode[res.size()]);
	}

	public Object getReference() {
		return fReference;
	}

	public BacklinkNode[] getBacklinkNodes() {
		return fBacklinkNodes;
	}

	@Override
	public String toString() {
		StringBuilder buf= new StringBuilder();
		buf.append(getReference().getClass().getName()).append('\n');
		for (BacklinkNode backlinkNode : getBacklinkNodes()) {
			buf.append("  ").append(backlinkNode.getBackLinkString());
			String value= backlinkNode.getValue().toString();
			if (value.length() > 100)
				value= value.substring(0, 99);
			buf.append(" -> ").append(value).append('\n');
		}
		return buf.toString();
	}

	private static class BacklinkNode {

		private final Object fValue;
		private final String fBackStrings;

		public BacklinkNode(String str, Object value) {
			fBackStrings= str;
			fValue= value;
		}

		public String getBackLinkString() {
			return fBackStrings;
		}

		public Object getValue() {
			return fValue;
		}

	}

}


