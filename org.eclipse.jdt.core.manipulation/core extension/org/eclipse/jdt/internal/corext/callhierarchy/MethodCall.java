/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.callhierarchy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.core.IMember;

public class MethodCall {
    private IMember fMember;
    private List<CallLocation> fCallLocations;
    private boolean potential;

    /**
     * @param enclosingElement
     */
    public MethodCall(IMember enclosingElement) {
    	this(enclosingElement, false);
    }

	/**
	 * @param enclosingElement enclosing member of this call object
	 * @param potential indicate whether this call object is a potential item in the hierarchy. A
	 *            item is considered as potential when there is no direct reference, like methods on
	 *            a implementation class which are referred through the interface in actual code.
	 */
	public MethodCall(IMember enclosingElement, boolean potential) {
		this.fMember= enclosingElement;
		this.potential = potential;
	}


    /**
     *
     */
    public Collection<CallLocation> getCallLocations() {
        return fCallLocations;
    }

    public CallLocation getFirstCallLocation() {
        if ((fCallLocations != null) && !fCallLocations.isEmpty()) {
            return fCallLocations.get(0);
        } else {
            return null;
        }
    }

    public boolean hasCallLocations() {
        return fCallLocations != null && fCallLocations.size() > 0;
    }

    /**
     * @return Object
     */
    public String getKey() {
        return getMember().getHandleIdentifier();
    }

    /**
     *
     */
    public IMember getMember() {
        return fMember;
    }

    /**
     * @param location
     */
    public void addCallLocation(CallLocation location) {
        if (fCallLocations == null) {
            fCallLocations = new ArrayList<>();
        }

        fCallLocations.add(location);
    }

	/**
	 * Returns if this is a potential call object.
	 *
	 * @return <code>true</code> if its potential.
	 * @see #MethodCall(IMember, boolean)
	 */
	public boolean isPotential() {
		return potential;
	}

	@Override
	public String toString() {
		StringBuilder builder= new StringBuilder();
		builder.append("MethodCall ["); //$NON-NLS-1$
		if (fMember != null) {
			builder.append(fMember);
		}
		builder.append(',').append(potential);
		builder.append("]"); //$NON-NLS-1$
		return builder.toString();
	}
}
