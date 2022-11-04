package test;

import among.Source;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public class ErrorTests{
	@TestFactory
	public List<DynamicTest> errorTests(){
		List<DynamicTest> list = new ArrayList<>();
		list.add(errorTest("unterminated1"));
		list.add(errorTest("unterminated2"));
		list.add(errorTest("unterminated3"));
		list.add(errorTest("expectValue1"));
		list.add(errorTest("invalidCharEscape"));
		list.add(errorTest("invalidMacro"));
		list.add(errorTest("invalidStatement"));
		list.add(errorTest("redundantComma"));
		list.add(errorTest("wtf"));
		list.add(errorTest("overlapWarn"));
		list.add(errorTest("sourcePosition"));
		return list;
	}

	@Test public void sourceReadIOException(){
		Assertions.assertThrows(IOException.class, () -> {
			Reader r = new StringReader("a");
			r.close();
			Source.read(r);
		}).printStackTrace();
	}

	private static DynamicTest errorTest(String name){
		return DynamicTest.dynamicTest(name, () -> TestUtil.expectError(TestUtil.expectSourceFrom("error_tests", name)));
	}
}
