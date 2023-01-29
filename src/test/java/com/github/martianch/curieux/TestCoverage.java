package com.github.martianch.curieux;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TestCoverage {
    public static void check(Class appClass, Class testingClass) {
        check(appClass, testingClass, exclude());
    }

    public static void check(Class appClass, Class testingClass, ExcludeList excludeList) {
        assertTrue("testingClass must end with \"Test\"", testingClass.getSimpleName().endsWith("Test"));
        var appMethodNamesToTest =
                Arrays.stream(appClass.getDeclaredMethods())
                        .map(Method::getName)
                        .filter(s -> !s.contains("$"))
                        .filter(excludeList::notExcluded)
                        .map(s -> s + "Test")
                        .sorted()
                        .distinct() // when we override a method to return a subtype, a new method with the same name appears
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

    public static ExcludeList exclude(String... names) {
        return new ExcludeList(names);
    }

    static class ExcludeList {
        private final Set<String> excludedNames;

        private ExcludeList(String[] names) {
            excludedNames = new HashSet<>(Arrays.asList(names));
        }

        public boolean notExcluded(String name) {
            return !excludedNames.contains(name);
        }
    }
}
