package org.eclipse.jdt.internal.ui.dialogs;/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */


import java.util.List;

public interface ISelectionValidator {
	void isValid(Object[] selection, StatusInfo res);
}