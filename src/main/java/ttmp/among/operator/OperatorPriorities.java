package ttmp.among.operator;

public interface OperatorPriorities{
	double BINARY_ASSIGN = 0;
	double BINARY_LOGICAL_OR = 1;
	double BINARY_LOGICAL_AND = 2;
	double BINARY_LOGICAL_EQUALITY = 3;
	double BINARY_LOGICAL_COMPARE = 4;
	double BINARY_BITWISE = 5;
	double BINARY_ARITHMETIC_ADDITION = 6;
	double BINARY_ARITHMETIC_PRODUCT = 7;
	double BINARY_ARITHMETIC_POWER = 8;
	double BINARY_CUSTOM = 9;
	double POSTFIX_CUSTOM = 10;
	double PREFIX = 11;
	double BINARY_ACCESS = 12;
}
