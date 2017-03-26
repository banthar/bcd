package bdc.test;

import org.junit.runner.RunWith;

@RunWith(BdcRunner.class)
public class ObjectsTest {

	@MethodReturnsConstant
	public static int alloc() {
		new Object();
		return 0;
	}

}
