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
package org.eclipse.jdt.ui.tests.refactoring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.junit.Assert;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.mapping.ModelProvider;

public class TestModelProvider extends ModelProvider {

	private static class Sorter implements Comparator<IResourceDelta> {
		@Override
		public int compare(IResourceDelta d1, IResourceDelta d2) {
			return d1.getResource().getFullPath().toPortableString().compareTo(
				d2.getResource().getFullPath().toPortableString());
		}
	}

	private static final Comparator<IResourceDelta> COMPARATOR= new Sorter();

	public static IResourceDelta LAST_DELTA;
	public static boolean IS_COPY_TEST;

	private static final int PRE_DELTA_FLAGS= IResourceDelta.CONTENT | IResourceDelta.MOVED_TO |
		IResourceDelta.MOVED_FROM | IResourceDelta.OPEN;

	public static void clearDelta() {
		LAST_DELTA= null;
	}

	@Override
	public IStatus validateChange(IResourceDelta delta, IProgressMonitor pm) {
		LAST_DELTA= delta;
		return super.validateChange(delta, pm);
	}

	public static void assertTrue(IResourceDelta expected) {
		Assert.assertNotNull(LAST_DELTA);
		boolean res= assertTrue(expected, LAST_DELTA);
		if (!res) {
			Assert.assertEquals(printDelta(expected), printDelta(LAST_DELTA));
		}

		LAST_DELTA= null;
	}

	private static boolean assertTrue(IResourceDelta expected, IResourceDelta actual) {
		assertEqual(expected.getResource(), actual.getResource());
		int actualKind= actual.getKind();
		int actualFlags= actual.getFlags();
		// The real delta can't combine kinds so we remove it from the received one as well.
		if ((actualKind & (IResourceDelta.ADDED | IResourceDelta.REMOVED)) != 0) {
			actualKind= actualKind & ~IResourceDelta.CHANGED;
		}

		// The expected delta doesn't support copy from flag. So remove it
		actualFlags= actualFlags & ~IResourceDelta.COPIED_FROM;

		int expectKind= expected.getKind();
		int expectedFlags= expected.getFlags() & PRE_DELTA_FLAGS;
		if ((expectKind & IResourceDelta.ADDED) != 0 && (expectedFlags & IResourceDelta.MOVED_FROM) != 0) {
			expectedFlags= expectedFlags & ~IResourceDelta.OPEN;
		}
		if (expectKind != actualKind || expectedFlags != actualFlags) {
			return false;
		}
		IResourceDelta[] expectedChildren=  getExpectedChildren(expected);
		IResourceDelta[] actualChildren= getActualChildren(actual, expectedChildren);
		if (expectedChildren.length != actualChildren.length) {
			return false;
		}
		Arrays.sort(expectedChildren, COMPARATOR);
		Arrays.sort(actualChildren, COMPARATOR);
		for (int i= 0; i < expectedChildren.length; i++) {
			boolean res= assertTrue(expectedChildren[i], actualChildren[i]);
			if (!res) {
				Assert.assertEquals(printDelta(expected), printDelta(actual));
			}
		}
		return true;
	}

	private static String printDelta(IResourceDelta delta) {
		StringBuffer buf= new StringBuffer();
		appendDelta(delta, 0, buf);
		return buf.toString();
	}

	private static StringBuffer appendDelta(IResourceDelta delta, int indent, StringBuffer buf) {
		for (int i= 0; i < indent; i++) {
			buf.append("  ");
		}
		buf.append(delta.getResource().toString());
		buf.append("-").append(getKindString(delta.getKind()));
		int flags= delta.getKind();
		if (flags != 0) {
			buf.append("-").append(getFlagString(flags)).append('\n');
		}

		IResourceDelta[] affectedChildren= delta.getAffectedChildren();
		Arrays.sort(affectedChildren, COMPARATOR);

		for (IResourceDelta resourcedelta : affectedChildren) {
			appendDelta(resourcedelta, indent + 1, buf);
		}
		return buf;
	}



	private static String getKindString(int kind) {
		switch (kind) {
			case IResourceDelta.CHANGED:
				return "CHANGED";
			case IResourceDelta.ADDED:
				return "ADDED";
			case IResourceDelta.REMOVED:
				return "REMOVED";
			case IResourceDelta.ADDED_PHANTOM:
				return "ADDED_PHANTOM";
			case IResourceDelta.REMOVED_PHANTOM:
				return "REMOVED_PHANTOM";
			default:
				return "NULL";
		}
	}

	private static String getFlagString(int flags) {
		StringBuffer buf= new StringBuffer();
		appendFlag(flags, IResourceDelta.CONTENT, "CONTENT", buf);
		appendFlag(flags, IResourceDelta.DESCRIPTION, "DESCRIPTION", buf);
		appendFlag(flags, IResourceDelta.ENCODING, "ENCODING", buf);
		appendFlag(flags, IResourceDelta.OPEN, "OPEN", buf);
		appendFlag(flags, IResourceDelta.MOVED_TO, "MOVED_TO", buf);
		appendFlag(flags, IResourceDelta.MOVED_FROM, "MOVED_FROM", buf);
		appendFlag(flags, IResourceDelta.TYPE, "TYPE", buf);
		appendFlag(flags, IResourceDelta.SYNC, "SYNC", buf);
		appendFlag(flags, IResourceDelta.MARKERS, "MARKERS", buf);
		appendFlag(flags, IResourceDelta.REPLACED, "REPLACED", buf);
		return buf.toString();
	}

	private static void appendFlag(int flags, int flag, String name, StringBuffer res) {
		if ((flags & flag) != 0) {
			if (res.length() > 0) {
				res.append("-");
			}
			res.append(name);
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
				if (expectedSegment.startsWith("UnusedName") || "unusedName".equals(expectedSegment))
					continue;
				Assert.assertEquals("Different path segment", expectedSegment, actualPath.segment(i));
			}
		} else {
			Assert.assertEquals("Same resource", expected, actual);
		}
	}

	private static IResourceDelta[] getExpectedChildren(IResourceDelta delta) {
		List<IResourceDelta> result= new ArrayList<>();
		for (IResourceDelta child : delta.getAffectedChildren()) {
			IResource resource= child.getResource();
			if (resource != null && isIgnorable(resource))
				continue;
			if (child.getAffectedChildren().length > 0) {
				result.add(child);
			} else {
				int flags= child.getFlags();
				if (flags == 0 || (flags & PRE_DELTA_FLAGS) != 0) {
					result.add(child);
				}
			}
		}
		return result.toArray(new IResourceDelta[result.size()]);
	}

	private static boolean isIgnorable(IResource resource) {
		final String name= resource.getName();
		if (resource.getType() != IResource.FOLDER)
			return false;
		return name.startsWith(".");
	}

	private static IResourceDelta[] getActualChildren(IResourceDelta delta, IResourceDelta[] expectedChildren) {
		List<IResourceDelta> result= new ArrayList<>();
		if (!IS_COPY_TEST) {
			for (IResourceDelta resourcedelta : delta.getAffectedChildren()) {
				IResource resource= resourcedelta.getResource();
				if (resource != null && isIgnorable(resource))
					continue;
				result.add(resourcedelta);
			}
		} else {
			for (IResourceDelta resourcedelta : delta.getAffectedChildren()) {
				IResource resource= resourcedelta.getResource();
				if (resource != null && isIgnorable(resource))
					continue;
				if (contains(expectedChildren, resourcedelta)) {
					result.add(resourcedelta);
				} else {
					assertCopySource(resourcedelta);
				}
			}
		}
		return result.toArray(new IResourceDelta[result.size()]);
	}

	private static boolean contains(IResourceDelta[] expectedChildren, IResourceDelta actualDelta) {
		IResource actualResource= actualDelta.getResource();
		for (IResourceDelta resourcedelta : expectedChildren) {
			if (isSameResourceInCopy(resourcedelta.getResource(), actualResource)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isSameResourceInCopy(IResource expected, IResource actual) {
		IPath expectedPath= expected.getFullPath();
		IPath actualPath= actual.getFullPath();
		if (expectedPath.segmentCount()!= actualPath.segmentCount())
			return false;
		for(int i= 0; i < expectedPath.segmentCount(); i++) {
			String expectedSegment= expectedPath.segment(i);
			if (expectedSegment.startsWith("UnusedName") || "unusedName".equals(expectedSegment))
				continue;
			if (!expectedSegment.equals(actualPath.segment(i)))
				return false;
		}
		return true;
	}

	private static void assertCopySource(IResourceDelta delta) {
		try {
			delta.accept(d -> {
				Assert.assertEquals("Not a copy delta", 0, (d.getKind() & ~IResourceDelta.CHANGED));
				return true;
			});
		} catch (CoreException e) {
			Assert.fail("Shouldn't happen");
		}
	}
}
