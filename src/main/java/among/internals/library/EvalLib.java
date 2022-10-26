package among.internals.library;

import among.ReportType;
import among.exception.Sussy;
import among.obj.Among;
import among.obj.AmongList;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

public final class EvalLib{
	private EvalLib(){}

	@Nullable public static Among eval(Among among, @Nullable BiConsumer<ReportType, String> reportHandler){
		if(among.isList()){
			AmongList list = among.asList();
			switch(list.getName()){
				case "||": return binaryBool(list, BinaryBoolOp.OR, reportHandler);
				case "&&": return binaryBool(list, BinaryBoolOp.AND, reportHandler);
				case "|": return binaryBool(list, BinaryBoolOp.OR_STRICT, reportHandler);
				case "&": return binaryBool(list, BinaryBoolOp.AND_STRICT, reportHandler);
				case "==": case "=": return binaryObj(list, BinaryObjOp.EQ, reportHandler);
				case "!=": return binaryObj(list, BinaryObjOp.NEQ, reportHandler);
				case ">": return binaryNum(list, BinaryNumOp.GT, reportHandler);
				case "<": return binaryNum(list, BinaryNumOp.LT, reportHandler);
				case ">=": return binaryNum(list, BinaryNumOp.GTEQ, reportHandler);
				case "<=": return binaryNum(list, BinaryNumOp.LTEQ, reportHandler);
				case "+": return list.size()<=1 ? unary(list, UnaryOp.PLUS, reportHandler) : binaryNum(list, BinaryNumOp.ADD, reportHandler);
				case "-": return list.size()<=1 ? unary(list, UnaryOp.NEGATE, reportHandler) : binaryNum(list, BinaryNumOp.SUB, reportHandler);
				case "*": return binaryNum(list, BinaryNumOp.MUL, reportHandler);
				case "/": return binaryNum(list, BinaryNumOp.DIV, reportHandler);
				case "^": case "**": return binaryNum(list, BinaryNumOp.POW, reportHandler);
				case "!": unary(list, UnaryOp.NOT, reportHandler);
			}
		}
		return among;
	}

	private enum BinaryObjOp{EQ, NEQ}
	@Nullable private static Among binaryObj(AmongList list, BinaryObjOp op, @Nullable BiConsumer<ReportType, String> reportHandler){
		if(expect(list, 2, reportHandler)){
			Among a = eval(list.get(0), reportHandler);
			if(a==null) return null;
			Among b = eval(list.get(1), reportHandler);
			if(b==null) return null;
			switch(op){
				case EQ: return Among.value(eq(a, b));
				case NEQ: return Among.value(!eq(a, b));
			}
		}
		return null;
	}

	public static boolean eq(Among a, Among b){
		if(a.equals(b)) return true;
		Boolean aBool = toBool(a);
		if(aBool!=null) return aBool.equals(toBool(b));
		Double aNum = toNum(a);
		if(aNum!=null) return aNum.equals(toNum(b));
		return false;
	}

	private enum BinaryBoolOp{AND, OR, AND_STRICT, OR_STRICT}
	@Nullable private static Among binaryBool(AmongList list, BinaryBoolOp op, @Nullable BiConsumer<ReportType, String> reportHandler){
		if(expect(list, 2, reportHandler)){
			Boolean a = evalBool(list.get(0), reportHandler);
			if(a==null) return null;
			switch(op){
				case AND_STRICT: if(!a) return Among.value(false); break;
				case OR_STRICT: if(a) return Among.value(true); break;
			}
			Boolean b = evalBool(list.get(1), reportHandler);
			if(b==null) return null;
			switch(op){
				case AND: case AND_STRICT: return Among.value(a&&b);
				case OR: case OR_STRICT: return Among.value(a||b);
			}
		}
		return null;
	}

	private enum BinaryNumOp{ADD, SUB, MUL, DIV, POW, GT, LT, GTEQ, LTEQ}
	@Nullable private static Among binaryNum(AmongList list, BinaryNumOp op, @Nullable BiConsumer<ReportType, String> reportHandler){
		if(expect(list, 2, reportHandler)){
			Double a = evalNum(list.get(0), reportHandler);
			if(a==null) return null;
			Double b = evalNum(list.get(1), reportHandler);
			if(b==null) return null;
			switch(op){
				case ADD: return Among.value(a+b);
				case SUB: return Among.value(a-b);
				case MUL: return Among.value(a*b);
				case DIV: return Among.value(a/b);
				case POW: return Among.value(Math.pow(a, b));
				case GT: return Among.value(a>b);
				case LT: return Among.value(a<b);
				case GTEQ: return Among.value(a>=b);
				case LTEQ: return Among.value(a<=b);
			}
		}
		return null;
	}

	private enum UnaryOp{NEGATE, PLUS, NOT}
	@Nullable private static Among unary(AmongList list, UnaryOp op, @Nullable BiConsumer<ReportType, String> reportHandler){
		if(expect(list, 1, reportHandler)){
			if(op==UnaryOp.NOT){
				Boolean a = evalBool(list.get(0), reportHandler);
				if(a==null) return null;
				return Among.value(!a);
			}
			Double a = evalNum(list.get(0), reportHandler);
			if(a==null) return null;
			switch(op){
				case NEGATE: return Among.value(-a);
				case PLUS: return Among.value(a);
			}
		}
		return null;
	}

	private static boolean expect(AmongList list, int size, @Nullable BiConsumer<ReportType, String> reportHandler){
		if(list.size()>=size) return true;
		if(reportHandler!=null)
			reportHandler.accept(ReportType.ERROR, "Invalid input '"+list+"': Not enough parameters, minimum "+size+" required");
		return false;
	}
	@Nullable private static Boolean evalBool(Among among, @Nullable BiConsumer<ReportType, String> reportHandler){
		Among a2 = eval(among, reportHandler);
		if(a2==null) return null;
		Boolean b = toBool(a2);
		if(b==null&&reportHandler!=null)
			reportHandler.accept(ReportType.ERROR, "Invalid input '"+a2+"': Expected boolean");
		return null;
	}
	@Nullable private static Double evalNum(Among among, @Nullable BiConsumer<ReportType, String> reportHandler){
		Among a2 = eval(among, reportHandler);
		if(a2==null) return null;
		Double n = toNum(a2);
		if(n==null&&reportHandler!=null)
			reportHandler.accept(ReportType.ERROR, "Invalid input '"+a2+"': Expected number");
		return n;
	}

	@Nullable public static Boolean toBool(Among among){
		if(among.isPrimitive()) try{
			return among.asPrimitive().getBoolValue();
		}catch(Sussy ignored){}
		return null;
	}
	@Nullable public static Double toNum(Among among){
		if(among.isPrimitive()) try{
			return among.asPrimitive().getDoubleValue();
		}catch(NumberFormatException ignored){}
		return null;
	}
}
