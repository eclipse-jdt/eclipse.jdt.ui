/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.text.tests;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.text.tests.Accessor;

import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.SourceViewer;

import org.eclipse.jdt.core.IJavaProject;

import org.eclipse.jdt.testplugin.JavaProjectHelper;
import org.eclipse.jdt.text.tests.performance.EditorTestHelper;
import org.eclipse.jdt.text.tests.performance.ResourceTestHelper;

import org.eclipse.jdt.ui.PreferenceConstants;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlighting;
import org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlightingManager;
import org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlightingPresenter;
import org.eclipse.jdt.internal.ui.javaeditor.SemanticHighlightings;


public class SemanticHighlightingTest extends TestCase {
	
	private static final Class THIS= SemanticHighlightingTest.class;
	
	private static class SemanticHighlightingTestSetup extends TestSetup {

		private IJavaProject fJavaProject;
		
		public SemanticHighlightingTestSetup(Test test) {
			super(test);
		}
		
		protected void setUp() throws Exception {
			super.setUp();
			fJavaProject= EditorTestHelper.createJavaProject(PROJECT, LINKED_FOLDER);
			
			disableAllSemanticHighlightings();
			
			fEditor= (JavaEditor) EditorTestHelper.openInEditor(ResourceTestHelper.findFile("/SHTest/src/SHTest.java"), true);
			fSourceViewer= EditorTestHelper.getSourceViewer(fEditor);
			assertTrue(EditorTestHelper.joinReconciler(fSourceViewer, 0, 10000, 100));
		}

		protected void tearDown () throws Exception {
			EditorTestHelper.closeEditor(fEditor);
			
			IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
			
			SemanticHighlighting[] semanticHighlightings= SemanticHighlightings.getSemanticHighlightings();
			for (int i= 0, n= semanticHighlightings.length; i < n; i++) {
				String enabledPreferenceKey= SemanticHighlightings.getEnabledPreferenceKey(semanticHighlightings[i]);
				if (!store.isDefault(enabledPreferenceKey))
					store.setToDefault(enabledPreferenceKey);
			}

			if (fJavaProject != null)
				JavaProjectHelper.delete(fJavaProject);
			
			super.tearDown();
		}
	}
	
	public static final String LINKED_FOLDER= "testResources/semanticHighlightingTest1";
	
	public static final String PROJECT= "SHTest";
	
	private static JavaEditor fEditor;
	
	private static SourceViewer fSourceViewer;

	public static Test suite() {
		return new SemanticHighlightingTestSetup(new TestSuite(THIS));
	}

	protected void setUp() throws Exception {
		super.setUp();
		disableAllSemanticHighlightings();
	}
	
	public void testDeprecatedMemberHighlighting() throws Exception {
		setUpSemanticHighlighting(SemanticHighlightings.DEPRECATED_MEMBER);
		Position[] expected= new Position[] {
				createPosition(0, 12, 13),
				createPosition(22, 5, 15),
				createPosition(24, 1, 13),
				createPosition(24, 15, 16),
				createPosition(25, 2, 15),
				createPosition(26, 2, 16),
				createPosition(27, 10, 10),
				createPosition(30, 7, 10),
				createPosition(30, 26, 13),
		};
		Position[] actual= getSemanticHighlightingPositions();
//		System.out.println(toString(actual));
		assertEqualPositions(expected, actual);
	}

	public void testStaticFinalFieldHighlighting() throws Exception {
		setUpSemanticHighlighting(SemanticHighlightings.STATIC_FINAL_FIELD);
		Position[] expected= new Position[] {
				createPosition(6, 18, 16),
				createPosition(35, 37, 16),
		};
		Position[] actual= getSemanticHighlightingPositions();
//		System.out.println(toString(actual));
		assertEqualPositions(expected, actual);
	}
	
	public void testStaticFieldHighlighting() throws Exception {
		setUpSemanticHighlighting(SemanticHighlightings.STATIC_FIELD);
		Position[] expected= new Position[] {
				createPosition(4, 12, 11),
				createPosition(6, 18, 16),
				createPosition(33, 32, 11),
				createPosition(35, 37, 16),
		};
		Position[] actual= getSemanticHighlightingPositions();
//		System.out.println(toString(actual));
		assertEqualPositions(expected, actual);
	}
	
	public void testFieldHighlighting() throws Exception {
		setUpSemanticHighlighting(SemanticHighlightings.FIELD);
		Position[] expected= new Position[] {
				createPosition(3, 5, 5),
				createPosition(4, 12, 11),
				createPosition(5, 11, 10),
				createPosition(6, 18, 16),
				createPosition(22, 5, 15),
				createPosition(25, 2, 15),
				createPosition(31, 9, 6),
				createPosition(32, 6, 11),
				createPosition(32, 31, 5),
				createPosition(33, 6, 17),
				createPosition(33, 32, 11),
				createPosition(34, 6, 16),
				createPosition(34, 36, 10),
				createPosition(35, 6, 22),
				createPosition(35, 37, 16),
		};
		Position[] actual= getSemanticHighlightingPositions();
//		System.out.println(toString(actual));
		assertEqualPositions(expected, actual);
	}
	
	public void testMethodDeclarationHighlighting() throws Exception {
		setUpSemanticHighlighting(SemanticHighlightings.METHOD_DECLARATION);
		Position[] expected= new Position[] {
				createPosition(7, 6, 6),
				createPosition(19, 13, 12),
				createPosition(20, 15, 14),
				createPosition(24, 15, 16),
				createPosition(40, 4, 6),
		};
		Position[] actual= getSemanticHighlightingPositions();
//		System.out.println(toString(actual));
		assertEqualPositions(expected, actual);
	}
	
	public void testStaticMethodInvocationHighlighting() throws Exception {
		setUpSemanticHighlighting(SemanticHighlightings.STATIC_METHOD_INVOCATION);
		Position[] expected= new Position[] {
				createPosition(10, 2, 12),
		};
		Position[] actual= getSemanticHighlightingPositions();
//		System.out.println(toString(actual));
		assertEqualPositions(expected, actual);
	}
	
	/*
	 * [syntax highlighting] 'Abstract Method Invocation' highlighting also matches declaration
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=73353
	 */
	public void testAbstractMethodInvocationHighlighting() throws Exception {
		setUpSemanticHighlighting(SemanticHighlightings.ABSTRACT_METHOD_INVOCATION);
		Position[] expected= new Position[] {
				createPosition(11, 2, 14),
		};
		Position[] actual= getSemanticHighlightingPositions();
//		System.out.println(toString(actual));
		assertEqualPositions(expected, actual);
	}
	
	public void testInheritedMethodInvocationHighlighting() throws Exception {
		setUpSemanticHighlighting(SemanticHighlightings.INHERITED_METHOD_INVOCATION);
		Position[] expected= new Position[] {
				createPosition(12, 2, 8),
				createPosition(15, 17, 8),
		};
		Position[] actual= getSemanticHighlightingPositions();
//		System.out.println(toString(actual));
		assertEqualPositions(expected, actual);
	}
	
	public void testLocalVariableDeclarationHighlighting() throws Exception {
		setUpSemanticHighlighting(SemanticHighlightings.LOCAL_VARIABLE_DECLARATION);
		Position[] expected= new Position[] {
				createPosition(7, 17, 5),
				createPosition(8, 6, 5),
				createPosition(13, 11, 6),
				createPosition(14, 26, 6),
		};
		Position[] actual= getSemanticHighlightingPositions();
//		System.out.println(toString(actual));
		assertEqualPositions(expected, actual);
	}
	
	public void testLocalVariableHighlighting() throws Exception {
		setUpSemanticHighlighting(SemanticHighlightings.LOCAL_VARIABLE);
		Position[] expected= new Position[] {
				createPosition(7, 17, 5),
				createPosition(8, 6, 5),
				createPosition(8, 13, 5),
				createPosition(9, 2, 5),
				createPosition(13, 11, 6),
				createPosition(13, 22, 6),
				createPosition(13, 35, 6),
				createPosition(14, 26, 6),
				createPosition(15, 3, 5),
				createPosition(15, 10, 6),
				createPosition(16, 3, 6),
		};
		Position[] actual= getSemanticHighlightingPositions();
//		System.out.println(toString(actual));
		assertEqualPositions(expected, actual);
	}
	
	public void testParameterVariableHighlighting() throws Exception {
		setUpSemanticHighlighting(SemanticHighlightings.PARAMETER_VARIABLE);
		Position[] actual= getSemanticHighlightingPositions();
		Position[] expected= new Position[] {
				createPosition(7, 17, 5),
				createPosition(8, 13, 5),
		};
//		System.out.println(toString(actual));
		assertEqualPositions(expected, actual);
	}

	public void testAnnotationElementHighlighting() throws Exception {
		setUpSemanticHighlighting(SemanticHighlightings.ANNOTATION_ELEMENT_REFERENCE);
		Position[] actual= getSemanticHighlightingPositions();
		Position[] expected= new Position[] {
				createPosition(38, 19, 5),
		};
//		System.out.println(toString(actual));
		assertEqualPositions(expected, actual);
	}

	public void testTypeParameterHighlighting() throws Exception {
		setUpSemanticHighlighting(SemanticHighlightings.TYPE_VARIABLE);
		Position[] actual= getSemanticHighlightingPositions();
		Position[] expected= new Position[] {
				createPosition(39, 15, 1),
				createPosition(40, 2, 1),
		};
//		System.out.println(toString(actual));
		assertEqualPositions(expected, actual);
	}

	private void assertEqualPositions(Position[] expected, Position[] actual) {
		assertEquals(expected.length, actual.length);
		for (int i= 0, n= expected.length; i < n; i++) {
			assertEquals(expected[i].isDeleted(), actual[i].isDeleted());
			assertEquals(expected[i].getOffset(), actual[i].getOffset());
			assertEquals(expected[i].getLength(), actual[i].getLength());
		}
	}

	private Position createPosition(int line, int column, int length) throws BadLocationException {
		IDocument document= fSourceViewer.getDocument();
		return new Position(document.getLineOffset(line) + column, length);
	}

	String toString(Position[] positions) throws BadLocationException {
		StringBuffer buf= new StringBuffer();
		IDocument document= fSourceViewer.getDocument();
		buf.append("Position[] expected= new Position[] {\n");
		for (int i= 0, n= positions.length; i < n; i++) {
			Position position= positions[i];
			int line= document.getLineOfOffset(position.getOffset());
			int column= position.getOffset() - document.getLineOffset(line);
			buf.append("\tcreatePosition(" + line + ", " + column + ", " + position.getLength() + "),\n");
		}
		buf.append("};\n");
		return buf.toString();
	}

	private Position[] getSemanticHighlightingPositions() throws BadPositionCategoryException {
		SemanticHighlightingManager manager= (SemanticHighlightingManager) new Accessor(fEditor, JavaEditor.class).get("fSemanticManager");
		SemanticHighlightingPresenter presenter= (SemanticHighlightingPresenter) new Accessor(manager, manager.getClass()).get("fPresenter");
		String positionCategory= (String) new Accessor(presenter, presenter.getClass()).invoke("getPositionCategory", new Object[0]);
		IDocument document= fSourceViewer.getDocument();
		return document.getPositions(positionCategory);
	}

	private void setUpSemanticHighlighting(String semanticHighlighting) {
		enableSemanticHighlighting(semanticHighlighting);
		EditorTestHelper.forceReconcile(fSourceViewer);
		assertTrue(EditorTestHelper.joinReconciler(fSourceViewer, 0, 10000, 100));
		EditorTestHelper.sleep(100);
		EditorTestHelper.runEventQueue();
	}

	private void enableSemanticHighlighting(String preferenceKey) {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		store.setValue(getEnabledPreferenceKey(preferenceKey), true);
	}

	private String getEnabledPreferenceKey(String preferenceKey) {
		return PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_PREFIX + preferenceKey + PreferenceConstants.EDITOR_SEMANTIC_HIGHLIGHTING_ENABLED_SUFFIX;
	}
	
	private static void disableAllSemanticHighlightings() {
		IPreferenceStore store= JavaPlugin.getDefault().getPreferenceStore();
		SemanticHighlighting[] semanticHilightings= SemanticHighlightings.getSemanticHighlightings();
		for (int i= 0, n= semanticHilightings.length; i < n; i++) {
			SemanticHighlighting semanticHilighting= semanticHilightings[i];
			if (store.getBoolean(SemanticHighlightings.getEnabledPreferenceKey(semanticHilighting)))
				store.setValue(SemanticHighlightings.getEnabledPreferenceKey(semanticHilighting), false);
		}
	}
}
