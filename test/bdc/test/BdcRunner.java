package bdc.test;

import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.Filterable;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;

import bdc.MethodType;
import bdc.ProgramTransformations;
import bdc.Type;
import bdc.Type.FieldType;
import bdc.URLClassParser;

public class BdcRunner extends Runner implements Filterable {

	private final Class<?> testClass;
	private List<Method> tests;

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
				final Object expectedValue = m.invoke(null, createRandomParameters(m.getParameters()));
				final List<FieldType> params = new ArrayList<>();
				for (final Parameter p : m.getParameters()) {
					params.add(Type.fromJavaClass(p.getType()));
				}
				final String descriptor = new MethodType(params, Type.fromJavaClass(m.getReturnType()))
						.toDescriptor();
				runTest(m.getName(), descriptor, expectedValue);
			} catch (final Throwable e) {
				runNotifier.fireTestFailure(new Failure(d, e));
			}
			runNotifier.fireTestFinished(d);
		}
	}

	private Object[] createRandomParameters(final Parameter[] parameters) {
		final Random random = new Random();
		final Object[] objects = new Object[parameters.length];
		for (int i = 0; i < parameters.length; i++) {
			final Class<?> type = parameters[i].getType();
			if (type == boolean.class) {
				objects[i] = random.nextBoolean();
			} else if (type == int.class) {
				objects[i] = random.nextInt();
			} else {
				throw new IllegalStateException("Unsupported type: " + type);
			}
		}
		return objects;
	}

	public void runTest(final String name, final String signature, final Object expectedValue) throws Exception {
		final List<URL> urls = new ArrayList<>();
		for (final String path : System.getProperty("sun.boot.class.path").split(File.pathSeparator)) {
			urls.add(createURL(path));
		}
		for (final String path : System.getProperty("java.class.path").split(File.pathSeparator)) {
			urls.add(createURL(path));
		}
		final URLClassParser loader = new URLClassParser(urls);
		final bdc.Class type = loader.loadClass(this.testClass.getName());
		final bdc.Method m = type.getMethod(name, signature);
		try {
			m.parse();
			ProgramTransformations.optimizeMainMethod(m);
			m.assertReturnsConstant(expectedValue);
		} catch (final Throwable e) {
			try {
				try (final PrintStream out = new PrintStream(new File(name + ".dot"))) {
					out.println("digraph G {");
					m.dumpRecursively(out);
					out.println("}");
				}
			} catch (final Throwable e1) {
				e.addSuppressed(e1);
				throw e;
			}
			throw e;
		}
	}

	private URL createURL(final String path) throws Exception {
		final File file = new File(path);
		if (file.isDirectory()) {
			return file.getAbsoluteFile().toURI().toURL();
		} else {
			return new URI("jar", file.getAbsoluteFile().toURI().toString() + "!/", null).toURL();
		}
	}

	@Override
	public void filter(final Filter filter) throws NoTestsRemainException {
		final List<Method> filteredTests = new ArrayList<>();
		for (final Method m : this.tests) {
			if (filter.shouldRun(Description.createTestDescription(this.testClass, m.getName()))) {
				filteredTests.add(m);
			}
		}
		this.tests = filteredTests;
		if (this.tests.isEmpty()) {
			throw new NoTestsRemainException();
		}
	}
}
