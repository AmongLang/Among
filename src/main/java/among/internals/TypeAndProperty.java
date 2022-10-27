package among.internals;

import among.operator.OperatorType;

final class TypeAndProperty{
	final OperatorType type;
	final byte properties;
	final double priority;

	TypeAndProperty(OperatorType type, byte properties, double priority){
		this.type = type;
		this.properties = properties;
		this.priority = priority;
	}
}
