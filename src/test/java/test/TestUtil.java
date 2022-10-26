package test;

import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import ttmp.among.AmongEngine;
import ttmp.among.CompileResult;
import ttmp.among.Source;
import ttmp.among.RootAndDefinition;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestUtil{
	public static final boolean log = true;
	private static final AmongEngine engine = new AmongEngine();

	public static RootAndDefinition make(String src){
		return make(Source.of(src));
	}
	public static RootAndDefinition make(Source src){
		long t = System.currentTimeMillis();
		CompileResult result = engine.read(src, null, null);
		t = System.currentTimeMillis()-t;
		result.printReports();
		result.expectSuccess();
		log(result.rootAndDefinition(), t);
		return result.rootAndDefinition();
	}

	public static void expectError(Source src){
		long t = System.currentTimeMillis();
		CompileResult result = engine.read(src, null, null);
		t = System.currentTimeMillis()-t;
		Assertions.assertFalse(result.isSuccess(), "Failed at failing smh");
		result.printReports();
		log(result.rootAndDefinition(), t);
	}

	public enum ExpectWarning{WARNING, NO_WARNING}
	public static void expectNoError(Source src, @Nullable ExpectWarning expectWarning){
		long t = System.currentTimeMillis();
		CompileResult result = engine.read(src, null, null);
		t = System.currentTimeMillis()-t;
		result.printReports();
		result.expectSuccess();
		if(expectWarning!=null) Assertions.assertEquals(result.hasWarning(), expectWarning==ExpectWarning.WARNING,
				expectWarning==ExpectWarning.WARNING ? "Expected warning" : "Unexpected warning");
		log(result.rootAndDefinition(), t);
	}

	public static Source expectSourceFrom(String folder, String fileName) throws IOException{
		String url = folder+"/"+fileName+".among";
		InputStream file = Thread.currentThread().getContextClassLoader().getResourceAsStream(url);
		assertNotNull(file, "File not found at '"+url+"'");
		return Source.read(new InputStreamReader(file, StandardCharsets.UTF_8));
	}
	@Nullable public static Source sourceFrom(String folder, String fileName) throws IOException{
		String url = folder+"/"+fileName+".among";
		InputStream file = Thread.currentThread().getContextClassLoader().getResourceAsStream(url);
		return file==null ? null : Source.read(new InputStreamReader(file, StandardCharsets.UTF_8));
	}

	public static void log(RootAndDefinition root, long time){
		if(log){
			System.out.println("Parsed in "+time+"ms");
			System.out.println("========== Compact String ==========");
			System.out.println(root);
			System.out.println("========== Pretty String ==========");
			System.out.println(root.toPrettyString());
		}
	}
}
