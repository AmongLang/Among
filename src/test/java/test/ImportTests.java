package test;

import org.junit.jupiter.api.Test;
import ttmp.among.AmongEngine;
import ttmp.among.AmongDefinition;
import ttmp.among.macro.Macro;
import ttmp.among.macro.MacroType;
import ttmp.among.obj.Among;
import ttmp.among.RootAndDefinition;

import static org.junit.jupiter.api.Assertions.*;
import static ttmp.among.obj.Among.*;

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
		RootAndDefinition root = engine.getOrReadFrom(name);
		t = System.currentTimeMillis()-t;
		assertNotNull(root, "Compilation failed");
		TestUtil.log(root, t, true);
		assertArrayEquals(expected, root.root().values().toArray(new Among[0]));
	}
	private static void err(AmongEngine engine, String name){
		assertNull(engine.getOrReadFrom(name), "Cannot even fail smh smh");
	}
}
