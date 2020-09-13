/*******************************************************************************
 * Copyright (c) 2011, 2019 IBM Corporation and others.
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
 *     Microsoft Corporation - copied the fields needed by jdt.core.manipulation
 *******************************************************************************/
package org.eclipse.jdt.core.refactoring.participants;

/**
 * Interface to define the processor IDs provided by JDT refactorings.
 *
 * <p>
 * This interface declares static final fields only; it is not intended to be
 * implemented.
 * </p>
 *
 * @since 1.4
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IRefactoringProcessorIds {

	/**
	 * Processor ID of the Change Method Signature processor
	 * (value <code>"org.eclipse.jdt.ui.changeMethodSignatureRefactoring"</code>).
	 *
	 * The Change Method Signature processor loads {@link ChangeMethodSignatureParticipant}s registered for the
	 * <code>IMethod</code> whose signature is changed.
	 */
	String CHANGE_METHOD_SIGNATURE_PROCESSOR= "org.eclipse.jdt.ui.changeMethodSignatureRefactoring"; //$NON-NLS-1$

	/**
	 * Processor ID of the Introduce Parameter Object processor
	 * (value <code>"org.eclipse.jdt.ui.introduceParameterObjectRefactoring"</code>).
	 *
	 * The Introduce Parameter Object processor loads {@link ChangeMethodSignatureParticipant}s registered for the
	 * <code>IMethod</code> whose signature is changed.
	 */
	String INTRODUCE_PARAMETER_OBJECT_PROCESSOR= "org.eclipse.jdt.ui.introduceParameterObjectRefactoring"; //$NON-NLS-1$

	/**
	 * Processor ID of the move static member processor
	 * (value <code>"org.eclipse.jdt.ui.MoveStaticMemberProcessor"</code>).
	 *
	 * The move static members processor loads participants registered for the
	 * static Java element that gets moved. No support is available to participate
	 * in non static member moves.
	 *
	 * @since 1.12
	 */
	String MOVE_STATIC_MEMBERS_PROCESSOR= "org.eclipse.jdt.ui.MoveStaticMemberProcessor"; //$NON-NLS-1$
}
