/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.corext.refactoring.base;


/**
 * A composite change consisting of a list of changes. Performing a composite
 * change peforms all managed changes. Managed changes can be either primitive
 * or composite changes.
 * Clients can implement this interface if they want their <code>IChange</code> to be treated as composites.
 * <p>
 * <bf>NOTE:<bf> This class/interface is part of an interim API that is still under development 
 * and expected to change significantly before reaching stability. It is being made available at 
 * this early stage to solicit feedback from pioneering adopters on the understanding that any 
 * code that uses this API will almost certainly be broken (repeatedly) as the API evolves.</p>
 */
public interface ICompositeChange extends IChange {

	/**
	 * Returns the set of changes this composite change consists of. If the composite
	 * change doesn't have any children, <code>null</code> is returned.
	 * @return an array of changes this composite change consists of
	 */
	public IChange[] getChildren();
}