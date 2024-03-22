/*******************************************************************************
 * Copyright (c) 2020, 2022 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Fabrice TIERCELIN - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ui.tests.quickfix;

import static org.junit.Assert.assertNotEquals;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.Rule;
import org.junit.Test;

import org.eclipse.core.runtime.CoreException;

import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;

import org.eclipse.jdt.ui.tests.core.rules.Java12ProjectTestSetup;
import org.eclipse.jdt.ui.tests.core.rules.ProjectTestSetup;

import org.eclipse.jdt.internal.ui.fix.MultiFixMessages;

/**
 * Tests the cleanup features related to Java 12.
 */
public class CleanUpTest12 extends CleanUpTestCase {
	@Rule
	public ProjectTestSetup projectSetup= new Java12ProjectTestSetup(false);

	@Override
	protected IJavaProject getProject() {
		return projectSetup.getProject();
	}

	@Override
	protected IClasspathEntry[] getDefaultClasspath() throws CoreException {
		return projectSetup.getDefaultClasspath();
	}

	@Test
	public void testSwitch() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String given= """
			package test1;
			
			public class E {
			    public static final int CONSTANT_1 = 0;
			    public static final int CONSTANT_2 = 1;
			
			    public int i2 = 0;
			
			    public void replaceIfWithSwitchOnParameter(int i1) {
			        int i = 0;
			        // Keep this comment
			        if (i1 == 0) {
			            // Keep this comment too
			            i = 0;
			            // Keep this comment also
			        } else if (i1 == 1) {
			            i = 10;
			        } else if (2 == i1) {
			            i = 20;
			        } else if (i1 == 3) {
			            i = 25;
			            i = 30;
			        } else if (i1 == 4)
			            i = 40;
			        else if ((i1 == 5) || (i1 == 6)) {
			            i = 60;
			        } else if ((i1 == 7) ^ (i1 == 8)) {
			            i = 80;
			        } else if ((i1 == 9) | (i1 == 10)) {
			            i = 100;
			        } else if ((i1 == 11) || i1 == 12 || (i1 == 13)) {
			            i = 130;
			        } else if (14 == i1) {
			            if (i2 == 1) {
			                i = 140;
			            }
			        } else if (i2 == 2) {
			            i = 150;
			        }
			    }
			
			    public void replaceIfWithSwitchUsingConstants(int date) {
			        int i = 0;
			        // Keep this comment
			        if (date == CONSTANT_1) {
			            // Keep this comment too
			            i = 0;
			            // Keep this comment also
			        } else if (CONSTANT_2 == date) {
			            i = 10;
			        } else if (date == 3) {
			            i = 60;
			        } else if (date == 4) {
			            i = 80;
			        } else {
			            i = 150;
			        }
			    }
			
			    public void replaceIfWithSwitchOnLocalVariable() {
			        int i1 = 0;
			        int i = 0;
			        // Keep this comment
			        if (i1 == 0) {
			            // Keep this comment too
			            i = 0;
			            // Keep this comment also
			        } else if (i1 == 1) {
			            i = 10;
			        } else if (2 == i1) {
			            i = 20;
			        } else if (i1 == 3) {
			            i = 25;
			            i = 30;
			        } else if (i1 == 5) {
			            // Do nothing
			        } else if (i1 == 4)
			            i = 40;
			        else {
			            i = 50;
			        }
			    }
			
			    public void replaceIfWithSwitchOnField() {
			        int i = 0;
			        // Keep this comment
			        if (i2 == 0) {
			            // Keep this comment too
			            i = 0;
			            // Keep this comment also
			        } else if (i2 == 1) {
			            i = 10;
			        } else if (i2 == 2) {
			            i = 20;
			        }
			    }
			
			    public void replaceWithSwitchOnField() {
			        int i = 0;
			        // Keep this comment
			        if (this.i2 == 0) {
			            // Keep this comment too
			            i = 0;
			            // Keep this comment also
			        } else if (this.i2 == 1) {
			            i = 10;
			        } else if (this.i2 == 2) {
			            i = 20;
			        }
			    }
			
			    public void replaceIfWithSwitchOnCharacter(char character) {
			        int i = 0;
			        // Keep this comment
			        if (character == 'a') {
			            // Keep this comment too
			            i = 0;
			            // Keep this comment also
			        } else if (character == 'b')
			            i = 10;
			        else if ('c' == character) {
			            i = 20;
			        } else if (character == 'd') {
			            i = 30;
			        } else
			            i = 40;
			    }
			
			    public void replaceIfRemoveDuplicateConditions(char aCharacter) {
			        int i = 0;
			        if (aCharacter == 'a') {
			            i = 0;
			        } else if (aCharacter == 'b') {
			            i = 10;
			        } else if (aCharacter == 'a') {
			            i = 20;
			        } else if (aCharacter == 'b') {
			            i = 30;
			        } else if ('c' == aCharacter) {
			            i = 40;
			        } else if (aCharacter == 'd' || aCharacter == 'b' || ('c' == aCharacter)) {
			            i = 50;
			        } else {
			            i = 60;
			        }
			    }
			
			    public void replaceIfWithSeveralConditions(char myCharacter) {
			        int i = 0;
			        if (myCharacter == 'a') {
			            i = 0;
			        } else if (myCharacter == 'z') {
			            i = 10;
			        } else if (myCharacter == 'a') {
			            i = 20;
			        } else if ((myCharacter == 'd') || (myCharacter == 'b') || ('c' == myCharacter) || ('f' == myCharacter)) {
			            i = 50;
			        } else {
			            i = 60;
			        }
			    }
			
			    public void replaceIfKeepExistingControlFlowBreaks(byte i1) {
			        byte j = 0;
			        loop: for (byte i = 0; i < 10; i++) {
			            if (i1 == 0) {
			                j = 0;
			                return;
			            } else if (i1 == 1) {
			                j = 10;
			                continue;
			            } else if (2 == i1) {
			                j = 20;
			                break loop;
			            } else if (i1 == 3) {
			                j = 25;
			                j = 30;
			            } else if (4 == i1) {
			                j = 40;
			                throw new RuntimeException();
			            } else if (5 == i1) {
			                j = 50;
			                if (i == 5) {
			                    throw new RuntimeException();
			                } else {
			                    return;
			                }
			            } else if (6 == i1) {
			                j = 60;
			                if (i == 5) {
			                    throw new RuntimeException();
			                }
			            }
			        }
			    }
			
			    public void replaceWithInnerLoopBreak(short i1) {
			        short j = 0;
			        if (i1 == 0) {
			            j = 0;
			        } else if (i1 == 1) {
			            j = 10;
			            short k = 0;
			            do {
			                if (j == i1) {
			                    break;
			                }
			                k++;
			            } while (k < j);
			        } else if (2 == i1) {
			            j = 20;
			            for (short l = 0; l < j; l++) {
			                if (j == i1) {
			                    break;
			                }
			            }
			        } else if (i1 == 3) {
			            j = 25;
			            j = 30;
			            short m = 0;
			            while (m < j) {
			                if (j == i1) {
			                    break;
			                }
			                m++;
			            }
			        } else if (4 == i1) {
			            j = 40;
			            for (short o : new short[] { 1, 2, 3 }) {
			                if (o == i1) {
			                    break;
			                }
			            }
			        } else if (5 == i1) {
			            j = 50;
			            switch (j) {
			            case 0 :
			                j = 0;
			                break;
			            case 1 :
			                j = 10;
			                break;
			            }
			        }
			    }
			
			    public void replaceIfWhenNoVariableNameConflictExists(int i1) {
			        int i = 0;
			        if (i1 == 0) {
			            int newVariable1 = 0;
			            i = newVariable1;
			        } else if (i1 == 1) {
			            int newVariable2 = 10;
			            i = newVariable2;
			        } else if (2 == i1) {
			            char newVariable3 = 'a';
			            i = newVariable3;
			        }
			    }
			
			    public void replaceWhenOutOfScopeVariableNameConflicts(int i1) {
			        int i = 0;
			        if (i1 == 0) {
			            for (int l = 0; l < i; l++) {
			                int integer1 = 0;
			                i = integer1;
			            }
			        } else if (i1 == 1) {
			            int integer1 = 10;
			            i = integer1;
			        } else if (i1 == 2) {
			            int i2 = 20;
			            i = i2;
			        }
			    }
			
			    public int replaceIfSuite(int i1) {
			        // Keep this comment
			        if (i1 == 0) {
			            // Keep this comment too
			            return 0;
			            // Keep this comment also
			        }
			        if (i1 == 1) {
			            return 10;
			        }
			        if (2 == i1) {
			            return 20;
			        }
			        if (i1 == 3) {
			            return 30;
			        }
			        if (i1 == 4)
			            return 40;
			        if ((i1 == 5) || (i1 == 6)) {
			            return 60;
			        }
			        if ((i1 == 7) ^ (i1 == 8)) {
			            return 80;
			        }
			        if ((i1 == 9) | (i1 == 10)) {
			            return 100;
			        }
			        if ((i1 == 11) || i1 == 12 || (i1 == 13)) {
			            return 130;
			        }
			        if (14 == i1) {
			            if (i2 == 1) {
			                return 140;
			            }
			            return 145;
			        }
			        return 155;
			    }
			
			    public int replaceSuiteThatDoNotFallThrough(int i1) {
			        if (i1 == 0) {
			            if (i2 == 1) {
			                return 0;
			            }
			        }
			        // Keep this comment
			        if (i1 == 1) {
			            // Keep this comment too
			            return 10;
			            // Keep this comment also
			        }
			        if (2 == i1) {
			            return 20;
			        }
			        if (i1 == 3) {
			            return 30;
			        }
			        if (i1 == 4)
			            return 40;
			        if ((i1 == 5) || (i1 == 6)) {
			            return 60;
			        }
			        if ((i1 == 7) ^ (i1 == 8)) {
			            return 80;
			        }
			        if ((i1 == 9) | (i1 == 10)) {
			            return 100;
			        }
			        if ((i1 == 11) || i1 == 12 || (i1 == 13)) {
			            return 130;
			        }
			        if (14 == i1) {
			            if (i2 == 1) {
			                return 140;
			            }
			            return 145;
			        }
			        return 155;
			    }
			
			    public int replaceSuiteIgnoring(int i1) {
			        if (i1 == 0) {
			            return 0;
			        } else if (i2 == 1) {
			            return 140;
			        }
			        // Keep this comment
			        if (i1 == 1) {
			            // Keep this comment too
			            return 10;
			            // Keep this comment also
			        }
			        if (2 == i1) {
			            return 20;
			        }
			        if (i1 == 3) {
			            return 30;
			        }
			        if (i1 == 4)
			            return 40;
			        if ((i1 == 5) || (i1 == 6)) {
			            return 60;
			        }
			        if ((i1 == 7) ^ (i1 == 8)) {
			            return 80;
			        }
			        if ((i1 == 9) | (i1 == 10)) {
			            return 100;
			        }
			        if ((i1 == 11) || i1 == 12 || (i1 == 13)) {
			            return 130;
			        }
			        if (14 == i1) {
			            if (i2 == 1) {
			                return 140;
			            }
			            return 145;
			        }
			        return 155;
			    }
			
			    public void replaceWhenVariableTypesConflict(int i1) {
			        int i = 0;
			        if (i1 == 0) {
			            int integer1 = 0;
			            i = integer1;
			        } else if (i1 == 2) {
			            char integer1 = 'a';
			            i = integer1;
			        } else if (i1 == 3) {
			            char c = 'a';
			            i = c;
			        } else {
			            char c = 'b';
			            i = c;
			        }
			    }
			
			    public int replaceMeltCases(int i1) {
			        // Keep this comment
			        if (i1 == 0) {
			            // Keep this comment too
			            return 0;
			            // Keep this comment also
			        } else if (i1 == 1) {
			            return 10;
			        } else if (2 == i1) {
			            return 20;
			        } else if (i1 == 3) {
			            return 30;
			        }
			        if (i1 == 4)
			            return 40;
			        if ((i1 == 5) || (i1 == 6)) {
			            return 60;
			        }
			        if ((i1 == 7) ^ (i1 == 8)) {
			            return 80;
			        }
			        if ((i1 == 9) | (i1 == 10)) {
			            return 100;
			        }
			        if ((i1 == 11) || i1 == 12 || (i1 == 13)) {
			            return 130;
			        }
			        if (14 == i1) {
			            if (i2 == 1) {
			                return 140;
			            }
			            return 145;
			        }
			        return 155;
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", given, false, null);

		enable(CleanUpConstants.USE_SWITCH);

		String expected= """
			package test1;
			
			public class E {
			    public static final int CONSTANT_1 = 0;
			    public static final int CONSTANT_2 = 1;
			
			    public int i2 = 0;
			
			    public void replaceIfWithSwitchOnParameter(int i1) {
			        int i = 0;
			        // Keep this comment
			        switch (i1) {
			            case 0 :
			                // Keep this comment too
			                i = 0;
			                // Keep this comment also
			                break;
			            case 1 :
			                i = 10;
			                break;
			            case 2 :
			                i = 20;
			                break;
			            case 3 :
			                i = 25;
			                i = 30;
			                break;
			            case 4 :
			                i = 40;
			                break;
			            case 5 :
			            case 6 :
			                i = 60;
			                break;
			            case 7 :
			            case 8 :
			                i = 80;
			                break;
			            case 9 :
			            case 10 :
			                i = 100;
			                break;
			            case 11 :
			            case 12 :
			            case 13 :
			                i = 130;
			                break;
			            case 14 :
			                if (i2 == 1) {
			                    i = 140;
			                }
			                break;
			            default :
			                if (i2 == 2) {
			                    i = 150;
			                }
			                break;
			        }
			    }
			
			    public void replaceIfWithSwitchUsingConstants(int date) {
			        int i = 0;
			        // Keep this comment
			        switch (date) {
			            case CONSTANT_1 :
			                // Keep this comment too
			                i = 0;
			                // Keep this comment also
			                break;
			            case CONSTANT_2 :
			                i = 10;
			                break;
			            case 3 :
			                i = 60;
			                break;
			            case 4 :
			                i = 80;
			                break;
			            default :
			                i = 150;
			                break;
			        }
			    }
			
			    public void replaceIfWithSwitchOnLocalVariable() {
			        int i1 = 0;
			        int i = 0;
			        // Keep this comment
			        switch (i1) {
			            case 0 :
			                // Keep this comment too
			                i = 0;
			                // Keep this comment also
			                break;
			            case 1 :
			                i = 10;
			                break;
			            case 2 :
			                i = 20;
			                break;
			            case 3 :
			                i = 25;
			                i = 30;
			                break;
			            case 5 :
			                break;
			            case 4 :
			                i = 40;
			                break;
			            default :
			                i = 50;
			                break;
			        }
			    }
			
			    public void replaceIfWithSwitchOnField() {
			        int i = 0;
			        // Keep this comment
			        switch (i2) {
			            case 0 :
			                // Keep this comment too
			                i = 0;
			                // Keep this comment also
			                break;
			            case 1 :
			                i = 10;
			                break;
			            case 2 :
			                i = 20;
			                break;
			            default :
			                break;
			        }
			    }
			
			    public void replaceWithSwitchOnField() {
			        int i = 0;
			        // Keep this comment
			        switch (this.i2) {
			            case 0 :
			                // Keep this comment too
			                i = 0;
			                // Keep this comment also
			                break;
			            case 1 :
			                i = 10;
			                break;
			            case 2 :
			                i = 20;
			                break;
			            default :
			                break;
			        }
			    }
			
			    public void replaceIfWithSwitchOnCharacter(char character) {
			        int i = 0;
			        // Keep this comment
			        switch (character) {
			            case 'a' :
			                // Keep this comment too
			                i = 0;
			                // Keep this comment also
			                break;
			            case 'b' :
			                i = 10;
			                break;
			            case 'c' :
			                i = 20;
			                break;
			            case 'd' :
			                i = 30;
			                break;
			            default :
			                i = 40;
			                break;
			        }
			    }
			
			    public void replaceIfRemoveDuplicateConditions(char aCharacter) {
			        int i = 0;
			        switch (aCharacter) {
			            case 'a' :
			                i = 0;
			                break;
			            case 'b' :
			                i = 10;
			                break;
			            case 'c' :
			                i = 40;
			                break;
			            case 'd' :
			                i = 50;
			                break;
			            default :
			                i = 60;
			                break;
			        }
			    }
			
			    public void replaceIfWithSeveralConditions(char myCharacter) {
			        int i = 0;
			        switch (myCharacter) {
			            case 'a' :
			                i = 0;
			                break;
			            case 'z' :
			                i = 10;
			                break;
			            case 'd' :
			            case 'b' :
			            case 'c' :
			            case 'f' :
			                i = 50;
			                break;
			            default :
			                i = 60;
			                break;
			        }
			    }
			
			    public void replaceIfKeepExistingControlFlowBreaks(byte i1) {
			        byte j = 0;
			        loop: for (byte i = 0; i < 10; i++) {
			            switch (i1) {
			                case 0 :
			                    j = 0;
			                    return;
			                case 1 :
			                    j = 10;
			                    continue;
			                case 2 :
			                    j = 20;
			                    break loop;
			                case 3 :
			                    j = 25;
			                    j = 30;
			                    break;
			                case 4 :
			                    j = 40;
			                    throw new RuntimeException();
			                case 5 :
			                    j = 50;
			                    if (i == 5) {
			                        throw new RuntimeException();
			                    } else {
			                        return;
			                    }
			                case 6 :
			                    j = 60;
			                    if (i == 5) {
			                        throw new RuntimeException();
			                    }
			                    break;
			                default :
			                    break;
			            }
			        }
			    }
			
			    public void replaceWithInnerLoopBreak(short i1) {
			        short j = 0;
			        switch (i1) {
			            case 0 :
			                j = 0;
			                break;
			            case 1 : {
			                j = 10;
			                short k = 0;
			                do {
			                    if (j == i1) {
			                        break;
			                    }
			                    k++;
			                } while (k < j);
			                break;
			            }
			            case 2 :
			                j = 20;
			                for (short l = 0; l < j; l++) {
			                    if (j == i1) {
			                        break;
			                    }
			                }
			                break;
			            case 3 : {
			                j = 25;
			                j = 30;
			                short m = 0;
			                while (m < j) {
			                    if (j == i1) {
			                        break;
			                    }
			                    m++;
			                }
			                break;
			            }
			            case 4 :
			                j = 40;
			                for (short o : new short[] { 1, 2, 3 }) {
			                    if (o == i1) {
			                        break;
			                    }
			                }
			                break;
			            case 5 :
			                j = 50;
			                switch (j) {
			                case 0 :
			                    j = 0;
			                    break;
			                case 1 :
			                    j = 10;
			                    break;
			                }
			                break;
			            default :
			                break;
			        }
			    }
			
			    public void replaceIfWhenNoVariableNameConflictExists(int i1) {
			        int i = 0;
			        switch (i1) {
			            case 0 : {
			                int newVariable1 = 0;
			                i = newVariable1;
			                break;
			            }
			            case 1 : {
			                int newVariable2 = 10;
			                i = newVariable2;
			                break;
			            }
			            case 2 : {
			                char newVariable3 = 'a';
			                i = newVariable3;
			                break;
			            }
			            default :
			                break;
			        }
			    }
			
			    public void replaceWhenOutOfScopeVariableNameConflicts(int i1) {
			        int i = 0;
			        switch (i1) {
			            case 0 :
			                for (int l = 0; l < i; l++) {
			                    int integer1 = 0;
			                    i = integer1;
			                }
			                break;
			            case 1 : {
			                int integer1 = 10;
			                i = integer1;
			                break;
			            }
			            case 2 : {
			                int i2 = 20;
			                i = i2;
			                break;
			            }
			            default :
			                break;
			        }
			    }
			
			    public int replaceIfSuite(int i1) {
			        // Keep this comment
			        switch (i1) {
			            case 0 :
			                // Keep this comment too
			                return 0;
			                // Keep this comment also
			            case 1 :
			                return 10;
			            case 2 :
			                return 20;
			            case 3 :
			                return 30;
			            case 4 :
			                return 40;
			            case 5 :
			            case 6 :
			                return 60;
			            case 7 :
			            case 8 :
			                return 80;
			            case 9 :
			            case 10 :
			                return 100;
			            case 11 :
			            case 12 :
			            case 13 :
			                return 130;
			            case 14 :
			                if (i2 == 1) {
			                    return 140;
			                }
			                return 145;
			            default :
			                break;
			        }
			        return 155;
			    }
			
			    public int replaceSuiteThatDoNotFallThrough(int i1) {
			        if (i1 == 0) {
			            if (i2 == 1) {
			                return 0;
			            }
			        }
			        // Keep this comment
			        switch (i1) {
			            case 1 :
			                // Keep this comment too
			                return 10;
			                // Keep this comment also
			            case 2 :
			                return 20;
			            case 3 :
			                return 30;
			            case 4 :
			                return 40;
			            case 5 :
			            case 6 :
			                return 60;
			            case 7 :
			            case 8 :
			                return 80;
			            case 9 :
			            case 10 :
			                return 100;
			            case 11 :
			            case 12 :
			            case 13 :
			                return 130;
			            case 14 :
			                if (i2 == 1) {
			                    return 140;
			                }
			                return 145;
			            default :
			                break;
			        }
			        return 155;
			    }
			
			    public int replaceSuiteIgnoring(int i1) {
			        if (i1 == 0) {
			            return 0;
			        } else if (i2 == 1) {
			            return 140;
			        }
			        // Keep this comment
			        switch (i1) {
			            case 1 :
			                // Keep this comment too
			                return 10;
			                // Keep this comment also
			            case 2 :
			                return 20;
			            case 3 :
			                return 30;
			            case 4 :
			                return 40;
			            case 5 :
			            case 6 :
			                return 60;
			            case 7 :
			            case 8 :
			                return 80;
			            case 9 :
			            case 10 :
			                return 100;
			            case 11 :
			            case 12 :
			            case 13 :
			                return 130;
			            case 14 :
			                if (i2 == 1) {
			                    return 140;
			                }
			                return 145;
			            default :
			                break;
			        }
			        return 155;
			    }
			
			    public void replaceWhenVariableTypesConflict(int i1) {
			        int i = 0;
			        switch (i1) {
			            case 0 : {
			                int integer1 = 0;
			                i = integer1;
			                break;
			            }
			            case 2 : {
			                char integer1 = 'a';
			                i = integer1;
			                break;
			            }
			            case 3 : {
			                char c = 'a';
			                i = c;
			                break;
			            }
			            default : {
			                char c = 'b';
			                i = c;
			                break;
			            }
			        }
			    }
			
			    public int replaceMeltCases(int i1) {
			        // Keep this comment
			        switch (i1) {
			            case 0 :
			                // Keep this comment too
			                return 0;
			                // Keep this comment also
			            case 1 :
			                return 10;
			            case 2 :
			                return 20;
			            case 3 :
			                return 30;
			            case 4 :
			                return 40;
			            case 5 :
			            case 6 :
			                return 60;
			            case 7 :
			            case 8 :
			                return 80;
			            case 9 :
			            case 10 :
			                return 100;
			            case 11 :
			            case 12 :
			            case 13 :
			                return 130;
			            case 14 :
			                if (i2 == 1) {
			                    return 140;
			                }
			                return 145;
			            default :
			                break;
			        }
			        return 155;
			    }
			}
			""";

		assertNotEquals("The class must be changed", given, expected);
		assertRefactoringResultAsExpected(new ICompilationUnit[] { cu }, new String[] { expected },
				new HashSet<>(Arrays.asList(MultiFixMessages.CodeStyleCleanUp_Switch_description)));
	}

	@Test
	public void testDoNotUseSwitch() throws Exception {
		IPackageFragment pack= fSourceFolder.createPackageFragment("test1", false, null);
		String sample= """
			package test1;
			
			public class E {
			    public void doNotReplaceWithOuterLoopBreak(int i1) {
			        int j = 0;
			        for (int i = 0; i < 10; i++) {
			            if (i1 == 0) {
			                j = 0;
			            } else if (i1 == 1) {
			                j = 10;
			            } else if (2 == i1) {
			                j = 20;
			            } else if (i1 == 3) {
			                j = 25;
			                j = 30;
			            } else if (4 == i1) {
			                j = 40;
			            } else if (5 == i1) {
			                j = 50;
			                break;
			            }
			        }
			    }
			
			    public void doNotReplaceIfWithoutElseIf(int i1) {
			        int i = 0;
			        if (i1 == 0) {
			            i = 0;
			        } else {
			            i = 10;
			        }
			    }
			
			    public void doNotReplaceIfWithoutElse(int i1) {
			        int i = 0;
			        if (i1 == 0) {
			            i = 10;
			        }
			    }
			
			    public void doNotReplaceWithSwitchOnPrimitiveWrapper(Integer i1) {
			        int i = 0;
			        if (i1 == 0) {
			            i = 0;
			        } else if (i1 == 10) {
			            i = 10;
			        } else if (i1 == 20) {
			            i = 20;
			        } else {
			            i = 30;
			        }
			    }
			
			    public void doNotRefactorLongVar(long l1) {
			        int i = 0;
			        if (l1 == 0) {
			            i = 0;
			        } else if (l1 == 1) {
			            i = 10;
			        } else if (l1 == 2) {
			            i = 20;
			        } else if (l1 == 3) {
			            i = 30;
			        }
			    }
			}
			""";
		ICompilationUnit cu= pack.createCompilationUnit("E.java", sample, false, null);

		enable(CleanUpConstants.USE_SWITCH);

		assertRefactoringHasNoChange(new ICompilationUnit[] { cu });
	}
}
