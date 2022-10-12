package test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ttmp.among.definition.AmongDefinition;
import ttmp.among.definition.Macro;
import ttmp.among.definition.MacroType;
import ttmp.among.definition.OperatorDefinition;
import ttmp.among.definition.OperatorPriorities;
import ttmp.among.definition.OperatorRegistry;
import ttmp.among.definition.OperatorType;
import ttmp.among.exception.Sussy;
import ttmp.among.obj.Among;
import ttmp.among.obj.AmongRoot;
import ttmp.among.util.NodePath;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static ttmp.among.definition.MacroReplacement.valueReplacement;
import static ttmp.among.obj.Among.*;

public class RecompileTests{
	@Test
	@DisplayName("Object 1")
	public void obj1(){
		recompileTest(value(3),
				value("The quick brown fox jumps over the lazy dog"),
				value("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut"+
						" labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco"+
						" laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in"+
						" voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat"+
						" cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."),
				value("The Industrial Revolution and its consequences have been a disaster for the\nhuman race."),
				value("According to all known laws of aviation, there is no way a bee should be able to fly.\n"+
						"Its wings are too small to get its fat little body off the ground.\n"+
						"The bee, of course, flies anyway because bees don't care what humans think is impossible.\n"+
						"Yellow, black. Yellow, black.\n"+
						"Yellow, black. Yellow, black.\n"+
						"Ooh, black and yellow!\n"+
						"Let's shake it up a l\n"));
	}
	@Test
	@DisplayName("Object 2")
	public void obj2(){
		recompileTest(object().prop("Hello", "World!"));
	}
	@Test
	@DisplayName("Object 3")
	public void obj3(){
		recompileTest(list(1, 2, 3));
	}
	@Test
	@DisplayName("Object 4")
	public void obj4(){
		recompileTest(object()
				.prop("L1", list(1, 2, 3, 4, 5))
				.prop("L2", namedList("Name2",
						namedList("1", 1, 2),
						namedList("2", 3, 4),
						namedList("3", 5, 6)))
				.prop("O1", namedObject("Obj"))
				.prop("O2", object())
				.prop("O3", namedObject("Values")
						.prop("A", "A")
						.prop("B", "B")
						.prop("C", "C")));
	}

	@Test
	@DisplayName("Object 5")
	public void obj5(){
		recompileTest(object()
						.prop("1", "ASDF;lc::::===12354\n\n\n**&&%%^&(/**/8*/%//{}{}[][]),,,,.,.,...///12#f\\\\\\\\!!!!!!")
						.prop("2", "Amogus\"); DROP TABLE Everything;/*")
						.prop("3", "////////// lol"),
				value("ASDF;lc::::===12354\n\n\n**&&%%^&(/**/8*/%//{}{}[][]),,,,.,.,...///12#f\\\\\\\\!!!!!!"),
				value("Amogus\"); DROP TABLE Everything;/*"),
				value("////////// lol"));
	}

	@Test
	@DisplayName("Macro 1")
	public void macro1(){
		recompileTest(
				Macro.builder().signature("macro", MacroType.CONST)
						.build(object().prop("Hello", "Macro!")));
	}

	@Test
	@DisplayName("Macro 2")
	public void macro2(){
		recompileTest(
				Macro.builder().signature("macro1", MacroType.LIST)
						.build(value("Macro with zero parameters")),
				Macro.builder().signature("macro2", MacroType.LIST)
						.param("1")
						.build(list(value("Macro with 1 parameter"), object()
								.prop("param 1", value("1"))
						), valueReplacement(NodePath.index(1).prop("param 1").of(), 0)),
				Macro.builder().signature("macro3", MacroType.LIST)
						.param("1")
						.param("2", value("default"))
						.build(list(value("Macro with 2 parameters"), object()
										.prop("param 1", value("1"))
										.prop("param 2", value("2"))
								), valueReplacement(NodePath.index(1).prop("param 1").of(), 0),
								valueReplacement(NodePath.index(1).prop("param 2").of(), 1)),
				Macro.builder().signature("macro4", MacroType.LIST)
						.param("1")
						.param("2", value("default"))
						.param("3", namedList("default 2", 1, 2, 3))
						.build(list(value("Macro with 3 parameters"), object()
										.prop("param 1", value("1"))
										.prop("param 2", value("2"))
										.prop("param 3", value("3"))
								), valueReplacement(NodePath.index(1).prop("param 1").of(), 0),
								valueReplacement(NodePath.index(1).prop("param 2").of(), 1),
								valueReplacement(NodePath.index(1).prop("param 3").of(), 2))
		);
	}

	@Test
	@DisplayName("Operator 1")
	public void op1(){
		recompileTest(new OperatorDefinition("yo", true, OperatorType.PREFIX));
	}

	@Test
	@DisplayName("Operator 2")
	public void op2(){
		recompileTest(
				new OperatorDefinition("yo", true, OperatorType.PREFIX),
				new OperatorDefinition("***", false, OperatorType.BINARY),
				new OperatorDefinition("..", false, OperatorType.BINARY, OperatorPriorities.BINARY_ACCESS)
		);
	}

	private static void recompileTest(Among... original){
			AmongRoot root = new AmongRoot();
			for(Among v : original) root.addObject(v);
			System.out.println("========== Original ==========");
			System.out.println(root.toPrettyString());
			System.out.println();
			System.out.println("Re-compiling toString() result");
			assertArrayEquals(original, TestUtil.make(root.toString()).root().objects().toArray(new Among[0]));
			System.out.println();
			System.out.println("Re-compiling toPrettyString() result");
			assertArrayEquals(original, TestUtil.make(root.toPrettyString()).root().objects().toArray(new Among[0]));
	}

	private static void recompileTest(Macro... original){
			AmongDefinition root = new AmongDefinition();
			for(Macro v : original){
				root.macros().add(v, (reportType, s) -> {
					throw new Sussy(s);
				});
			}
			System.out.println("========== Original ==========");
			System.out.println(root.toPrettyString());
			System.out.println();
			System.out.println("Re-compiling toString() result");
			assertEquals(root.macros(), TestUtil.make(root.toString()).definition().macros());
			System.out.println();
			System.out.println("Re-compiling toPrettyString() result");
			assertEquals(root.macros(), TestUtil.make(root.toPrettyString()).definition().macros());
	}

	private static void recompileTest(OperatorDefinition... original){
			AmongDefinition root = new AmongDefinition();
			for(OperatorDefinition v : original){
				OperatorRegistry.RegistrationResult r = root.operators().add(v);
				if(!r.isSuccess())
					throw new RuntimeException("Cannot register operator '"+v+"': "+r.message(v));
			}
			System.out.println("========== Original ==========");
			System.out.println(root.toPrettyString());
			System.out.println();
			System.out.println("Re-compiling toString() result");
			assertEquals(root.operators(), TestUtil.make(root.toString()).definition().operators());
			System.out.println();
			System.out.println("Re-compiling toPrettyString() result");
			assertEquals(root.operators(), TestUtil.make(root.toPrettyString()).definition().operators());
	}
}
