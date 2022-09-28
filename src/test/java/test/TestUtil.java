package test;

import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import ttmp.among.AmongEngine;
import ttmp.among.compile.CompileResult;
import ttmp.among.compile.Source;
import ttmp.among.obj.AmongRoot;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestUtil{
	public static final boolean log = true;
	private static final AmongEngine engine = new AmongEngine();

	public static AmongRoot make(String src){
		return make(Source.of(src));
	}
	public static AmongRoot make(Source src){
		return make(src, null);
	}

	public static AmongRoot make(String src, AmongRoot root){
		return make(Source.of(src), root);
	}
	public static AmongRoot make(Source src, AmongRoot root){
		long t = System.currentTimeMillis();
		CompileResult result = engine.read(src, root);
		t = System.currentTimeMillis()-t;
		result.printReports();
		root = result.expectSuccess();
		log(root, t);
		return root;
	}

	public static AmongRoot expectError(String src){
		return expectError(Source.of(src));
	}
	public static AmongRoot expectError(Source src){
		return expectError(src, null);
	}

	public static AmongRoot expectError(String src, AmongRoot root){
		return expectError(Source.of(src), root);
	}
	public static AmongRoot expectError(Source src, AmongRoot root){
		long t = System.currentTimeMillis();
		CompileResult result = engine.read(src, root);
		t = System.currentTimeMillis()-t;
		Assertions.assertFalse(result.isSuccess(), "Failed at failing smh");
		result.printReports();
		log(result.root(), t);
		return result.root();
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

	public static void log(AmongRoot root, long time){
		if(log){
			System.out.println("Parsed in "+time+"ms");
			System.out.println("========== Compact String ==========");
			System.out.println(root);
			System.out.println("========== Pretty String ==========");
			System.out.println(root.objectsAndDefinitionsToPrettyString());
		}
	}
}
