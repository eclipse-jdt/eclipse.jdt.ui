/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.core.refactoring.fields;

import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.refactoring.Refactoring;
import org.eclipse.jdt.core.search.IJavaSearchScope;

import org.eclipse.jdt.internal.core.refactoring.Assert;

/*
 * non java-doc
 * not API
 */
abstract class FieldRefactoring extends Refactoring {

	private IField fField;
	
	public FieldRefactoring(IField field){
		super();
		fField= field;
	}
	
	public FieldRefactoring(IJavaSearchScope scope, IField field){
		super(scope);
		fField= field;
		Assert.isTrue(field.exists(), "field must exist");
	}
		
	public final IField getField() {
		return fField;
	}
}