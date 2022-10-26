package test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ttmp.among.AmongDefinition;
import ttmp.among.macro.Macro;
import ttmp.among.macro.MacroReplacement;
import ttmp.among.macro.MacroType;
import ttmp.among.operator.OperatorType;
import ttmp.among.exception.Sussy;
import ttmp.among.AmongRoot;
import ttmp.among.internals.library.DefaultInstanceProvider;
import ttmp.among.NodePath;

import static ttmp.among.obj.Among.*;

public class CopyTests{
	@Test public void copyRoot(){
		AmongRoot r1 = new AmongRoot();
		r1.addObject(value("asdfadvbvkcl'''n\";safa;f"));
		r1.addObject(namedList("asdfadvbvkcl'''n;safa;f", "1213sfadvadfcv"));

		System.out.println("========== Original ==========");
		System.out.println(r1.toPrettyString());

		AmongRoot r2 = r1.copy();
		System.out.println("========== Copy ==========");
		System.out.println(r2.toPrettyString());

		Assertions.assertEquals(r1.objects(), r2.objects());
	}
	@Test public void copyDefinition(){
		AmongDefinition def = new AmongDefinition();
		def.macros().add(Macro.builder("This is macro", MacroType.CONST)
				.build(object()
						.prop("P1", "1")
						.prop("P2", "2")), (t, s) -> {
			throw new Sussy(s);
		});
		def.macros().add(Macro.builder("Macro2", MacroType.OBJECT)
				.param("p1")
				.param("p2", value("default"))
				.param("p3")
				.build(object()
								.prop("P1", value("p1"))
								.prop("P2", value("p2"))
								.prop("P3", value("p3")),
						MacroReplacement.valueReplacement(NodePath.prop("P1").of(), 0),
						MacroReplacement.valueReplacement(NodePath.prop("P2").of(), 1),
						MacroReplacement.valueReplacement(NodePath.prop("P3").of(), 2)
				));

		def.operators().addOperator("~~", OperatorType.PREFIX);
		def.operators().addOperator("~~", OperatorType.POSTFIX);
		def.operators().addKeyword("sus", OperatorType.POSTFIX);
		def.operators().addKeyword("x", OperatorType.BINARY);

		System.out.println("========== Original ==========");
		System.out.println(def.toPrettyString());

		AmongDefinition def2 = def.copy();
		System.out.println("========== Copy ==========");
		System.out.println(def2.toPrettyString());

		Assertions.assertEquals(def.macros(), def2.macros());
		Assertions.assertEquals(def.operators(), def2.operators());
	}

	@Test public void defaultOperators(){
		AmongDefinition def = DefaultInstanceProvider.defaultOperators();

		System.out.println("========== Original ==========");
		System.out.println(def.toPrettyString());

		AmongDefinition def2 = def.copy();
		System.out.println("========== Copy ==========");
		System.out.println(def2.toPrettyString());

		Assertions.assertEquals(def.operators(), def2.operators());
	}
}
