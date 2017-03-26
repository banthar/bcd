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
		;

		public static PureTransformationType fromTargetType(final PrimitiveType to) {
			throw new IllegalStateException();
		}

	}

}