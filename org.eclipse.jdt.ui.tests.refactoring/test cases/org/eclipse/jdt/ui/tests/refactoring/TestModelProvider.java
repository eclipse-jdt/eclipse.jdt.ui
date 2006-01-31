/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.refactoring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.mapping.ModelProvider;

public class TestModelProvider extends ModelProvider {
	
	private static class Sorter implements Comparator {
		public int compare(Object o1, Object o2) {
			IResourceDelta d1= (IResourceDelta) o1;
			IResourceDelta d2= (IResourceDelta) o2;
			return d1.getResource().getFullPath().toPortableString().compareTo(
				d2.getResource().getFullPath().toPortableString());
		}
	}
	
	private static class Status {
		private int kind;
		private int flags;
		public Status(int kind, int flags) {
			this.kind= kind;
			this.flags= flags;
		}
		public int hashCode() {
			final int PRIME= 31;
			int result= 1;
			result= PRIME * result + kind;
			result= PRIME * result + flags;
			return result;
		}
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null || getClass() != obj.getClass())
				return false;
			final Status other= (Status) obj;
			return kind == other.kind && flags == other.flags;
		}
		public String toString() {
			return "Status: kind(" + kind + ") flags(" + flags + ")";
		}
	}
	
	private static final Comparator COMPARATOR= new Sorter();
	
	public static IResourceDelta LAST_DELTA;
	public static boolean IS_COPY_TEST;

	private static final int PRE_DELTA_FLAGS= IResourceDelta.CONTENT | IResourceDelta.MOVED_TO | 
		IResourceDelta.MOVED_FROM | IResourceDelta.OPEN; 
	
	public static void clearDelta() {
		LAST_DELTA= null;
	}
	
	public IStatus validateChange(IResourceDelta delta, IProgressMonitor pm) {
		LAST_DELTA= delta;
		return super.validateChange(delta, pm);
	}
	
	public static void assertTrue(IResourceDelta expected) {
		Map expectedPostProcess= new HashMap();
		Map actualPostProcess= new HashMap();
		Assert.assertNotNull(LAST_DELTA);
		assertTrue(expected, LAST_DELTA, expectedPostProcess, actualPostProcess);
		for (Iterator iter= expectedPostProcess.keySet().iterator(); iter.hasNext();) {
			IPath resource= (IPath) iter.next();
			Status expectedValue= (Status) expectedPostProcess.get(resource);
			Status actualValue= (Status) actualPostProcess.get(resource);
			Assert.assertEquals("Same status value", expectedValue, actualValue);
		}
		LAST_DELTA= null;
	}
	
	private static void assertTrue(IResourceDelta expected, IResourceDelta actual, Map expectedPostProcess, Map actualPostProcess) {
		assertEqual(expected.getResource(), actual.getResource());
		int actualKind= actual.getKind();
		int actualFlags= actual.getFlags();
		// The real delta can't combine kinds so we remove it from the received one as well.
		if ((actualKind & (IResourceDelta.ADDED | IResourceDelta.REMOVED)) != 0) {
			actualKind= actualKind & ~IResourceDelta.CHANGED;
		}
		
		// The expected delta doesn't support copy from flag. So remove it
		actualFlags= actualFlags & ~IResourceDelta.COPIED_FROM;
		
		// There is a difference between a pre and a post delta. For example if I change and
		// move a resource then in the pre delta we have the change on the MOVE_TO whereas in
		// the post delta we have the changed on the MOVED_FROM.
		if ((actualKind & IResourceDelta.REMOVED) != 0 && (actualFlags & IResourceDelta.MOVED_TO) != 0) {
			IPath moveTo= actual.getMovedToPath();
			actualPostProcess.put(moveTo, new Status(IResourceDelta.ADDED, (actualFlags & ~IResourceDelta.MOVED_TO) | IResourceDelta.MOVED_FROM));
			actualFlags= actualFlags & ~IResourceDelta.CONTENT;
		}
		int expectKind= expected.getKind();
		int expectedFlags= expected.getFlags() & PRE_DELTA_FLAGS;
		if ((expectKind & IResourceDelta.ADDED) != 0 && (expectedFlags & IResourceDelta.MOVED_FROM) != 0) {
			expectedFlags= expectedFlags & ~IResourceDelta.OPEN;
			expectedPostProcess.put(expected.getResource().getFullPath(), new Status(expectKind, expectedFlags));
			expectedFlags= expectedFlags & ~IResourceDelta.CONTENT;
		}
		Assert.assertEquals("Same kind", expectKind, actualKind);
		Assert.assertEquals("Same flags", expectedFlags, actualFlags);
		IResourceDelta[] expectedChildren=  getExpectedChildren(expected);
		IResourceDelta[] actualChildren= actual.getAffectedChildren();
		Assert.assertEquals("Same number of children", expectedChildren.length, actualChildren.length);
		Arrays.sort(expectedChildren, COMPARATOR);
		Arrays.sort(actualChildren, COMPARATOR);
		for (int i= 0; i < expectedChildren.length; i++) {
			assertTrue(expectedChildren[i], actualChildren[i], expectedPostProcess, actualPostProcess);
		}
	}

	private static void assertEqual(IResource expected, IResource actual) {
		// This is a simple approach to deal with renamed resources in the deltas.
		// However it will not work if there is more than on child per delta since
		// the children will be sorted and their order might change.
		if (IS_COPY_TEST) {
			IPath expectedPath= expected.getFullPath();
			IPath actualPath= actual.getFullPath();
			Assert.assertEquals("Same path length", expectedPath.segmentCount(), actualPath.segmentCount());
			for(int i= 0; i < expectedPath.segmentCount(); i++) {
				String expectedSegment= expectedPath.segment(i);
				if (expectedSegment.startsWith("UnusedName") || expectedSegment.equals("unusedName"))
					continue;
				Assert.assertEquals("Different path segment", expectedSegment, actualPath.segment(i));
			}
		} else {
			Assert.assertEquals("Same resource", expected, actual);
		}
	}
	
	private static IResourceDelta[] getExpectedChildren(IResourceDelta delta) {
		List result= new ArrayList();
		IResourceDelta[] children= delta.getAffectedChildren();
		for (int i= 0; i < children.length; i++) {
			IResourceDelta child= children[i];
			if (child.getAffectedChildren().length > 0) {
				result.add(child);
			} else {
				int flags= child.getFlags();
				if (flags == 0 || (flags & PRE_DELTA_FLAGS) != 0) {
					result.add(child);
				}
			}
		}
		return (IResourceDelta[]) result.toArray(new IResourceDelta[result.size()]);
	}
}