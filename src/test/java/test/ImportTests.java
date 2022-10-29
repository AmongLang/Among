package test;

import among.AmongDefinition;
import among.AmongEngine;
import among.ReadResult;
import among.RootAndDefinition;
import among.macro.Macro;
import among.macro.MacroType;
import among.obj.Among;
import org.junit.jupiter.api.Test;

import static among.obj.Among.*;
import static org.junit.jupiter.api.Assertions.*;

public class ImportTests{
	AmongEngine engine = new AmongEngine();

	{
		engine.addSourceProvider(path -> TestUtil.sourceFrom("import_tests", path));
		engine.addInstanceProvider(path -> {
			switch(path){
				case "provided_instance/1":{
					AmongDefinition definition = new AmongDefinition();
					definition.macros().add(Macro.builder("filename", MacroType.OPERATION)
							.build(value("Provided Instance #1")));
					return new RootAndDefinition(definition);
				}
				case "provided_instance/2":{
					AmongDefinition definition = new AmongDefinition();
					definition.macros().add(Macro.builder("filename", MacroType.OPERATION)
							.build(value("Provided Instance #2")));
					return new RootAndDefinition(definition);
				}
				default: return null;
			}
		});
	}

	@Test public void defOp(){
		eq(engine, "defOp",
				value("+"),
				value("++"),
				value("+++"),
				namedList("+", 21),
				namedList("+", namedList("+", 1, 3), 5));
	}

	@Test public void importTest1(){
		eq(engine, "importTest1",
				namedObject("none")
						.prop("filename", namedList("filename"))
						.prop("number", "NUMBER"),
				namedObject("1")
						.prop("filename", "import_tests/import1.among")
						.prop("number", 1),
				namedObject("2")
						.prop("filename", "import_tests/import2.among")
						.prop("number", 2));
	}

	@Test public void importTest2(){
		eq(engine, "importTest2",
				value("coolMacro"),
				value("coolMacro"),
				value("Cool Macro"),
				value("coolMacro"),
				value("Cool Macro"));
	}

	@Test public void importTest3(){
		eq(engine, "importTest3",
				value("Provided Instance #1"),
				value("Provided Instance #2"),
				value("Provided Instance #1"),
				value("Provided Instance #2"));
	}

	@Test public void invalidRef(){
		err(engine, "invalidRef");
	}
	@Test public void selfRef(){
		err(engine, "selfRef");
	}
	@Test public void circRef(){
		err(engine, "circRef1");
	}

	private static void eq(AmongEngine engine, String name, Among... expected){
		long t = System.currentTimeMillis();
		ReadResult root = engine.getOrReadFrom(name);
		t = System.currentTimeMillis()-t;
		assertTrue(root.isSuccess(), "Compilation failed");
		TestUtil.log(root.rootAndDefinition(), t, true);
		assertArrayEquals(expected, root.root().values().toArray(new Among[0]));
	}
	private static void err(AmongEngine engine, String name){
		assertFalse(engine.getOrReadFrom(name).isSuccess(), "Cannot even fail smh smh");
	}
}
