package bdc;

import bdc.Type.PrimitiveType;

public interface PureTransformationType {

	enum Negate implements PureTransformationType {
		NEGATE
	}

	enum Compare implements PureTransformationType {
		COMPARE
	}

	enum Convert implements PureTransformationType {
		LONG;

		public static PureTransformationType fromTargetType(final PrimitiveType to) {
			switch (to) {
			case Long:
				return Convert.LONG;
			}
			throw new IllegalStateException("Unsupported target type: " + to);
		}

	}

}