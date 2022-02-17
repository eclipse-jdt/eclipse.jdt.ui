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
 * 			(report 36180: Callers/Callees view)
 *   Red Hat Inc. - refactored to jdt.core.manipulation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.callhierarchy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

import org.eclipse.jdt.internal.core.manipulation.JavaManipulationPlugin;

/**
 * The main plugin class to be used in the desktop.
 */
public class Implementors {
    private static IImplementorFinder[] IMPLEMENTOR_FINDERS= new IImplementorFinder[] { new JavaImplementorFinder() };
    private static Implementors fgInstance;

    /**
     * Returns the shared instance.
     */
    public static Implementors getInstance() {
        if (fgInstance == null) {
            fgInstance = new Implementors();
        }

        return fgInstance;
    }

    /**
     * Searches for implementors of the specified Java elements. Currently, only IMethod
     * instances are searched for. Also, only the first element of the elements
     * parameter is taken into consideration.
     *
     * @param elements
     *
     * @return An array of found implementing Java elements (currently only IMethod
     *         instances)
     */
    public IJavaElement[] searchForImplementors(IJavaElement[] elements,
        IProgressMonitor progressMonitor) {
        if ((elements != null) && (elements.length > 0)) {
            IJavaElement element = elements[0];

            try {
                if (element instanceof IMember) {
                    IMember member = (IMember) element;
                    IType type = member.getDeclaringType();

                    if (type.isInterface() || Flags.isAbstract(type.getFlags())) {
                        IType[] implementingTypes = findImplementingTypes(type,
                                progressMonitor);

                        if (member.getElementType() == IJavaElement.METHOD) {
                            return findMethods((IMethod)member, implementingTypes, progressMonitor);
                        } else {
                            return implementingTypes;
                        }
                    }
                }
            } catch (JavaModelException e) {
                JavaManipulationPlugin.log(e);
            }
        }

        return null;
    }

    /**
     * Searches for interfaces which are implemented by the declaring classes of the
     * specified Java elements. Currently, only IMethod instances are searched for.
     * Also, only the first element of the elements parameter is taken into
     * consideration.
     *
     * @param elements
     *
     * @return An array of found interfaces implemented by the declaring classes of the
     *         specified Java elements (currently only IMethod instances)
     */
    public IJavaElement[] searchForInterfaces(IJavaElement[] elements,
        IProgressMonitor progressMonitor) {
        if ((elements != null) && (elements.length > 0)) {
            IJavaElement element = elements[0];

            if (element instanceof IMember) {
                IMember member = (IMember) element;
                IType type = member.getDeclaringType();

                IType[] implementingTypes = findInterfaces(type, progressMonitor);

                if (!progressMonitor.isCanceled()) {
                    if (member.getElementType() == IJavaElement.METHOD) {
                        return findMethods((IMethod)member, implementingTypes, progressMonitor);
                    } else {
                        return implementingTypes;
                    }
                }
            }
        }

        return null;
    }

    private IImplementorFinder[] getImplementorFinders() {
        return IMPLEMENTOR_FINDERS;
    }

    private IType[] findImplementingTypes(IType type, IProgressMonitor progressMonitor) {
        Collection<IType> implementingTypes = new ArrayList<>();

        IImplementorFinder[] finders = getImplementorFinders();

        for (int i = 0; (i < finders.length) && !progressMonitor.isCanceled(); i++) {
            Collection<IType> types = finders[i].findImplementingTypes(type,
                    new SubProgressMonitor(progressMonitor, 10,
                        SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));

            if (types != null) {
                implementingTypes.addAll(types);
            }
        }

        return implementingTypes.toArray(new IType[implementingTypes.size()]);
    }

    private IType[] findInterfaces(IType type, IProgressMonitor progressMonitor) {
        Collection<IType> interfaces = new ArrayList<>();

        IImplementorFinder[] finders = getImplementorFinders();

        for (int i = 0; (i < finders.length) && !progressMonitor.isCanceled(); i++) {
            Collection<IType> types = finders[i].findInterfaces(type,
                    new SubProgressMonitor(progressMonitor, 10,
                        SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));

            if (types != null) {
                interfaces.addAll(types);
            }
        }

        return interfaces.toArray(new IType[interfaces.size()]);
    }

    /**
     * Finds IMethod instances on the specified IType instances with identical signatures
     * as the specified IMethod parameter.
     *
     * @param method The method to find "equals" of.
     * @param types The types in which the search is performed.
     *
     * @return An array of methods which match the method parameter.
     */
    private IJavaElement[] findMethods(IMethod method, IType[] types,
        IProgressMonitor progressMonitor) {
        Collection<IMethod> foundMethods = new ArrayList<>();

        SubProgressMonitor subProgressMonitor = new SubProgressMonitor(progressMonitor,
                10, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL);
        subProgressMonitor.beginTask("", types.length); //$NON-NLS-1$

        try {
            for (IType type : types) {
            	// we don't want the type we start the searched on be searched again. This happens when
            	// searching for abstract method implementations on abstract classes.
            	if(method.getDeclaringType().equals(type)) {
            		continue;
            	}

                IMethod[] methods = type.findMethods(method);

                if (methods != null) {
					foundMethods.addAll(Arrays.asList(methods));
                }

                subProgressMonitor.worked(1);
            }
        } finally {
            subProgressMonitor.done();
        }

        return foundMethods.toArray(new IJavaElement[foundMethods.size()]);
    }
}
