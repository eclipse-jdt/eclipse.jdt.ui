/*******************************************************************************
 * Copyright (c) 2002 International Business Machines Corp. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.jdt.internal.ui.actions;

import org.eclipse.ui.actions.LabelRetargetAction;
import org.eclipse.ui.actions.RetargetAction;


public class ActionDelegates {
	
	// Navigate menu
	
	public static class OpenAction extends RetargetActionDelegator {
		protected RetargetAction createRetargetAction() {
			return new LabelRetargetAction(getId(), ActionMessages.getString("OpenAction.label")); //$NON-NLS-1$
		}
		protected String getId() {
			return RetargetActionIDs.OPEN;
		}
	}
	public static class OpenSuperImplementation extends RetargetActionDelegator {
		protected String getId() {
			return RetargetActionIDs.OPEN_SUPER_IMPLEMENTATION;
		}
	}
	public static class OpenExternalJavadoc extends RetargetActionDelegator {
		protected String getId() {
			return RetargetActionIDs.OPEN_EXTERNAL_JAVA_DOC;
		}
	}
	public static class OpenTypeHierarchy extends RetargetActionDelegator {
		protected String getId() {
			return RetargetActionIDs.OPEN_TYPE_HIERARCHY;
		}
	}
	public static class ShowInPackageView extends RetargetActionDelegator {
		protected String getId() {
			return RetargetActionIDs.SHOW_IN_PACKAGE_VIEW;
		}
	}
	public static class ShowInNavigatorView extends RetargetActionDelegator {
		protected String getId() {
			return RetargetActionIDs.SHOW_IN_NAVIGATOR_VIEW;
		}
	}
	public static class ShowNextProblem extends RetargetActionDelegator {
		protected String getId() {
			return RetargetActionIDs.SHOW_NEXT_PROBLEM;
		}
	}
	public static class ShowPreviousProblem extends RetargetActionDelegator {
		protected String getId() {
			return RetargetActionIDs.SHOW_PREVIOUS_PROBLEM;
		}
	}
	
	// Source menu	
	
	public static class ContentAssist extends RetargetActionDelegator {
		protected String getId() {
			return RetargetActionIDs.CONTENT_ASSIST;
		}
	}
	public static class CorrectionAssist extends RetargetActionDelegator {
		protected String getId() {
			return RetargetActionIDs.CORRECTION_ASSIST;
		}
	}
	public static class ContentAssistContextInformation extends RetargetActionDelegator {
		protected String getId() {
			return RetargetActionIDs.CONTENT_ASSIST_CONTEXT_INFORMATION;
		}
	}
	public static class ShowJavaDoc extends RetargetActionDelegator {
		protected String getId() {
			return RetargetActionIDs.SHOW_JAVA_DOC;
		}
	}
	public static class Comment extends RetargetActionDelegator {
		protected String getId() {
			return RetargetActionIDs.COMMENT;
		}
	}
	public static class Uncomment extends RetargetActionDelegator {
		protected String getId() {
			return RetargetActionIDs.UNCOMMENT;
		}
	}
	public static class ShiftRight extends RetargetActionDelegator {
		protected String getId() {
			return RetargetActionIDs.SHIFT_RIGHT;
		}
	}
	public static class ShiftLeft extends RetargetActionDelegator {
		protected String getId() {
			return RetargetActionIDs.SHIFT_LEFT;
		}
	}
	public static class Format extends RetargetActionDelegator {
		protected String getId() {
			return RetargetActionIDs.FORMAT;
		}
	}
	public static class AddImport extends RetargetActionDelegator {
		protected String getId() {
			return RetargetActionIDs.ADD_IMPORT;
		}
	}
	public static class OrganizeImports extends RetargetActionDelegator {
		protected String getId() {
			return RetargetActionIDs.ORGANIZE_IMPORTS;
		}
	}
	public static class SurroundWithTryCatch extends RetargetActionDelegator {
		protected String getId() {
			return RetargetActionIDs.SURROUND_WITH_TRY_CATCH;
		}
	}
	public static class OverrideMethods extends RetargetActionDelegator {
		protected String getId() {
			return RetargetActionIDs.OVERRIDE_METHODS;
		}
	}
	public static class GenerateGetterSetter extends RetargetActionDelegator {
		protected String getId() {
			return RetargetActionIDs.GENERATE_GETTER_SETTER;
		}
	}
	public static class AddConstructorFromSuperclass extends RetargetActionDelegator {
		protected String getId() {
			return RetargetActionIDs.ADD_CONSTRUCTOR_FROM_SUPERCLASS;
		}
	}
	public static class AddJavaDocComment extends RetargetActionDelegator {
		protected String getId() {
			return RetargetActionIDs.ADD_JAVA_DOC_COMMENT;
		}
	}
	public static class FindStringsToExternalize extends RetargetActionDelegator {
		protected String getId() {
			return RetargetActionIDs.FIND_STRINGS_TO_EXTERNALIZE;
		}
	}
	public static class ExternalizeStrings extends RetargetActionDelegator {
		protected String getId() {
			return RetargetActionIDs.EXTERNALIZE_STRINGS;
		}
	}
	
	// Expand Selection menu
	
	public static class SelectEnclosingElement extends RetargetActionDelegator {
		protected String getId() {
			return RetargetActionIDs.SELECT_ENCLOSING_ELEMENT;
		}
	}
	public static class SelectNextElement extends RetargetActionDelegator {
		protected String getId() {
			return RetargetActionIDs.SELECT_NEXT_ELEMENT;
		}
	}
	public static class SelectPreviousElement extends RetargetActionDelegator {
		protected String getId() {
			return RetargetActionIDs.SELECT_PREVIOUS_ELEMENT;
		}
	}
	public static class RestoreLastSelection extends RetargetActionDelegator {
		protected String getId() {
			return RetargetActionIDs.RESTORE_LAST_SELECTION;
		}
	}
	
	// Refactor menu
	public static class ModifyParameters extends RetargetActionDelegator {
		protected String getId() {
			return RetargetActionIDs.MODIFY_PARAMETERS;
		}
	}

	public static class PullUp extends RetargetActionDelegator {
		protected String getId() {
			return RetargetActionIDs.PULL_UP;
		}
	}
	
}
