/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.jdt.ui.tests;

import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;

public class InheritedTestsFilter extends Filter {
    @Override
    public boolean shouldRun(Description description) {
        Class<?> clazz = description.getTestClass();
        String methodName = description.getMethodName();
        if (clazz.isAnnotationPresent(IgnoreInheritedTests.class)) {
            try {
                return clazz.getDeclaredMethod(methodName) != null;
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String describe() {
        return "Filter out tests on super class";
    }
}
