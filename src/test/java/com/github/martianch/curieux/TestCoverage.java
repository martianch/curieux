package com.github.martianch.curieux;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TestCoverage {
    public static void check(Class appClass, Class testingClass) {
        assertTrue("testingClass must end with \"Test\"", testingClass.getSimpleName().endsWith("Test"));
        var appMethodNamesToTest =
                Arrays.stream(appClass.getDeclaredMethods())
                        .map(Method::getName)
                        .filter(s -> !s.contains("$"))
                        .map(s -> s + "Test")
                        .collect(Collectors.toList());
        var testingMethodNames =
                Arrays.stream(testingClass.getDeclaredMethods())
                        .map(Method::getName)
                        .filter(s -> s.endsWith("Test"))
                        .collect(Collectors.toSet());
        var missingMethods = appMethodNamesToTest.stream()
                .filter(methodName -> !testingMethodNames.contains(methodName))
                .collect(Collectors.joining(", "));
        if (!missingMethods.isEmpty()) {
            fail("missing test methods in " + testingClass.getSimpleName() + ": " + missingMethods);
        }
    }
}
