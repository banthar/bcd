package bdc.test;

import org.junit.runner.RunWith;

@RunWith(BdcRunner.class)
public class ObjectsTest {

	@MethodReturnsConstant
	public static int alloc() {
		new Object();
		return 0;
	}

	@MethodReturnsConstant
	public static int read_field_default_constructor() {
		return new ClassWithDefaultConstructor().value;
	}

	@MethodReturnsConstant
	public static int write_and_read_field_default_constructor() {
		final ClassWithDefaultConstructor object = new ClassWithDefaultConstructor();
		object.value = 1;
		return object.value;
	}

	@MethodReturnsConstant
	public static int read_field_custom_constructor() {
		return new ClassWithCustomConstructor().value;
	}

	@MethodReturnsConstant
	public static int write_and_read_field_custom_constructor() {
		final ClassWithCustomConstructor object = new ClassWithCustomConstructor();
		object.value = 1;
		return object.value;
	}

	private static class ClassWithDefaultConstructor {
		public int value;
	}

	private static class ClassWithCustomConstructor {
		public int value = 1;
	}

}
