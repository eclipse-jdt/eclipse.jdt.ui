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
 *   Jesper Kamstrup Linnet (eclipse@kamstrup-linnet.dk) - initial API and implementation
 *          (report 36180: Callers/Callees view)
 *   Red Hat Inc. - refactored to jdt.core.manipulation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.callhierarchy;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;

import org.eclipse.jdt.internal.core.manipulation.JavaManipulationPlugin;

class CalleeMethodWrapper extends MethodWrapper {
    private Comparator<MethodWrapper> fMethodWrapperComparator = new MethodWrapperComparator();

    private static class MethodWrapperComparator implements Comparator<MethodWrapper> {
        @Override
		public int compare(MethodWrapper m1, MethodWrapper m2) {
            CallLocation callLocation1 = m1.getMethodCall().getFirstCallLocation();
            CallLocation callLocation2 = m2.getMethodCall().getFirstCallLocation();

            if ((callLocation1 != null) && (callLocation2 != null)) {
                if (callLocation1.getStart() == callLocation2.getStart()) {
                    return callLocation1.getEnd() - callLocation2.getEnd();
                }

                return callLocation1.getStart() - callLocation2.getStart();
            }

            return 0;
        }
    }

    public CalleeMethodWrapper(MethodWrapper parent, MethodCall methodCall) {
        super(parent, methodCall);
    }

	/* Returns the calls sorted after the call location
	 * @see org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper#getCalls()
     */
    @Override
	public MethodWrapper[] getCalls(IProgressMonitor progressMonitor) {
        MethodWrapper[] result = super.getCalls(progressMonitor);
        Arrays.sort(result, fMethodWrapperComparator);

        return result;
    }

    @Override
	protected String getTaskName() {
        return CallHierarchyMessages.CalleeMethodWrapper_taskname;
    }

	/*
	 * @see org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper#createMethodWrapper(org.eclipse.jdt.internal.corext.callhierarchy.MethodCall)
     */
    @Override
	protected MethodWrapper createMethodWrapper(MethodCall methodCall) {
        return new CalleeMethodWrapper(this, methodCall);
    }

    /*
     * @see org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper#canHaveChildren()
     */
    @Override
	public boolean canHaveChildren() {
    	return true;
    }

	/**
     * Find callees called from the current method.
	 * @see org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper#findChildren(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
	protected Map<String, MethodCall> findChildren(IProgressMonitor progressMonitor) {
    	IMember member= getMember();
		if (member.exists()) {
			CompilationUnit cu= CallHierarchyCore.getCompilationUnitNode(member, true);
		    if (progressMonitor != null) {
		        progressMonitor.worked(5);
		    }

			if (cu != null) {
				CalleeAnalyzerVisitor visitor = new CalleeAnalyzerVisitor(this.getMethodCall().getFirstCallLocation(), member, cu, progressMonitor);

				cu.accept(visitor);
				return visitor.getCallees();
			} else {
				return findCalleesFromParticipants(member, progressMonitor);
			}
		}
        return new HashMap<>(0);
    }

	private Map<String, MethodCall> findCalleesFromParticipants(IMember member, IProgressMonitor monitor) {
		try {
			String path= null;
			if (member.getResource() != null) {
				path= member.getResource().getFullPath().toString();
			} else if (member.getPath() != null) {
				path= member.getPath().toString();
			}
			if (path == null)
				return new HashMap<>(0);

			CallSearchResultCollector collector= new CallSearchResultCollector();
			SearchParticipant[] participants= SearchEngine.getSearchParticipants();

			for (SearchParticipant participant : participants) {
				SearchMatch[] calleeMatches= participant.locateCallees(
						member, participant.getDocument(path), monitor);

				for (SearchMatch match : calleeMatches) {
					if (match.getElement() instanceof IMember callee) {
						IMember resolved= resolveCallee(callee, monitor);
						if (resolved != null) {
							collector.addMember(member, resolved,
									match.getOffset(),
									match.getOffset() + match.getLength());
						}
					}
				}
			}
			return collector.getCallers();
		} catch (CoreException e) {
			JavaManipulationPlugin.log(e);
			return new HashMap<>(0);
		}
	}

	private IMember resolveCallee(IMember callee, IProgressMonitor monitor) {
		if (callee.exists()) {
			return callee;
		}
		int searchFor;
		switch (callee.getElementType()) {
			case IJavaElement.METHOD:
				searchFor= IJavaSearchConstants.METHOD;
				break;
			case IJavaElement.FIELD:
				searchFor= IJavaSearchConstants.FIELD;
				break;
			default:
				searchFor= IJavaSearchConstants.TYPE;
				break;
		}
		// Extract call site context from standard JDT interfaces
		int argCount= -1;
		String receiverTypeFQN= null;
		String[] argTypes= null;
		if (callee instanceof IMethod method) {
			argCount= method.getNumberOfParameters();
			argTypes= method.getParameterTypes();
			IType declType= method.getDeclaringType();
			if (declType != null) {
				receiverTypeFQN= declType.getFullyQualifiedName();
			}
		}
		return CallHierarchyCore.findFirstDeclaration(
				callee.getElementName(), searchFor,
				CallHierarchyCore.getDefault().getSearchScope(), monitor,
				argCount, receiverTypeFQN, null, argTypes);
	}
}
