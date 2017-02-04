package bdc.test;

import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;

import bdc.URLClassParser;

public class BdcRunner extends Runner {

	private final Class<?> testClass;
	private final List<Method> tests;

	public BdcRunner(final Class<?> testClass) {
		this.testClass = testClass;
		this.tests = new ArrayList<>();
		for (final Method m : this.testClass.getDeclaredMethods()) {
			final MethodReturnsConstant annotation = m.getAnnotation(MethodReturnsConstant.class);
			if (annotation != null) {
				this.tests.add(m);
			}
		}
	}

	@Override
	public Description getDescription() {
		final Description classDescription = Description.createSuiteDescription(this.testClass);
		for (final Method m : this.tests) {
			classDescription.addChild(Description.createTestDescription(this.testClass, m.getName()));
		}
		return classDescription;
	}

	@Override
	public void run(final RunNotifier runNotifier) {
		for (final Method m : this.tests) {
			final Description d = Description.createTestDescription(this.testClass, m.getName());
			runNotifier.fireTestStarted(d);
			try {
				final int expectedValue = (Integer) m.invoke(null);
				runTest(m.getName(), expectedValue);
			} catch (final Throwable e) {
				runNotifier.fireTestFailure(new Failure(d, e));
			}
			runNotifier.fireTestFinished(d);
		}
	}

	public void runTest(final String name, final int expectedValue) throws Exception {
		final URLClassParser loader = new URLClassParser(new URL[] { this.testClass.getResource("/") });
		final bdc.Class type = loader.loadClass(this.testClass.getName());
		final bdc.Method m = type.getMethod(name, "()I");
		m.parse();
		try {
			m.assertReturnsConstantInt(expectedValue);
		} catch (final Throwable e) {
			try (final PrintStream out = new PrintStream(new File(name + ".dot"))) {
				out.println("digraph G {");
				m.dump(out);
				out.println("}");
			}
			throw e;
		}
	}

	private class TestMethod {
		private final String name;
		private final int expectedValue;

		public TestMethod(final String name, final int expectedValue) {
			this.name = name;
			this.expectedValue = expectedValue;
		}

		public String getName() {
			return this.name;
		}
	}
}
