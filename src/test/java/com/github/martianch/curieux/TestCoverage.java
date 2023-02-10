package com.github.martianch.curieux;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * An excellent utility that checks if all methods defined in an interface
 * have been correctly implemented (well, covered by unit tests,
 * it is unit tests that check correctness). The problem is that the
 * inherited method implementations happen to be incorrect for some classes.
 * Such methods must be redefined in the classes, but today's tools do
 * not warn you if you forget to redefine them.
 */
public class TestCoverage {
    public static void check(Class appClass, Class testingClass) {
        check(appClass, testingClass, exclude());
    }

    public static void check(Class appClass, Class testingClass, ExcludeList excludeList) {
        var appMethodNamesToTest =
                Arrays.stream(appClass.getDeclaredMethods())
                        //.filter(m -> !Modifier.isStatic(m.getModifiers()))
                        .map(Method::getName)
                        .filter(s -> !s.contains("$"))
                        .filter(excludeList::notExcluded)
                        .collect(Collectors.toList());
        checkNameList(testingClass, appMethodNamesToTest);
    }

    public static void checkNames(Class testingClass, String... appMethodNamesToTest) {
        checkNameList(testingClass, Arrays.asList(appMethodNamesToTest));
    }

    private static void checkNameList(Class testingClass, List<String> appMethodNamesToTest) {
        assertTrue("testingClass must end with \"Test\"", testingClass.getSimpleName().endsWith("Test"));
        var testMethodNamesToTest =
                        appMethodNamesToTest.stream()
                        .map(s -> s + "Test")
                        .sorted()
                        .distinct() // when we override a method to return a subtype, a new method with the same name appears
                        .collect(Collectors.toList());
        var testingMethodNames =
                Arrays.stream(testingClass.getDeclaredMethods())
                        .map(Method::getName)
                        .filter(s -> s.endsWith("Test"))
                        .collect(Collectors.toSet());
        var missingMethods = testMethodNamesToTest.stream()
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
