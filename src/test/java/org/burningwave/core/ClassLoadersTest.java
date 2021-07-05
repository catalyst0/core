package org.burningwave.core;

import static org.burningwave.core.assembler.StaticComponentContainer.ClassLoaders;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.burningwave.core.assembler.ComponentSupplier;
import org.burningwave.core.classes.ByteCodeHunter.SearchResult;
import org.burningwave.core.classes.Classes;
import org.burningwave.core.classes.JavaClass;
import org.burningwave.core.classes.MemoryClassLoader;
import org.burningwave.core.classes.SearchConfig;
import org.burningwave.core.io.PathHelper;
import org.junit.jupiter.api.Test;

public class ClassLoadersTest extends BaseTest {
	
	protected MemoryClassLoader getMemoryClassLoader(ClassLoader parent) {
		return MemoryClassLoader.create(
			parent
		);
	}
	
	@Test
	public void setAsParentClassLoaderTest() {
		testNotNull(() -> {
			try (MemoryClassLoader classLoader = getMemoryClassLoader(null);) {
				ClassLoaders.setAsParent(classLoader, Thread.currentThread().getContextClassLoader());
				return ClassLoaders.getParent(classLoader);
			}
		});
	}
	
	@Test
	public void setAsMasterTest() {
		testNotNull(() -> {
			MemoryClassLoader classLoader_1 = getMemoryClassLoader(null);
			MemoryClassLoader classLoader_2 = getMemoryClassLoader(classLoader_1);
			MemoryClassLoader classLoader_3 = getMemoryClassLoader(classLoader_2);
			MemoryClassLoader classLoader_4 = getMemoryClassLoader(null);
			Function<Boolean, ClassLoader> resetter = ClassLoaders.setAsMaster(classLoader_3, classLoader_4);
			ClassLoader parent = ClassLoaders.getParent(classLoader_1);
			resetter.apply(true);
			classLoader_4.close();
			classLoader_3.close();
			classLoader_2.close();
			classLoader_1.close();
			return parent;
		});
	}
	
	@Test
	public void getAsParentClassLoaderTest() {
		testNotEmpty(() -> {
			try(MemoryClassLoader classLoader = getMemoryClassLoader(null)) {
				ClassLoaders.setAsParent(classLoader, Thread.currentThread().getContextClassLoader());
				return ClassLoaders.getAllParents(classLoader);
			}
		});
	}
	
	@Test
	public void retrieveLoadedClassesForPackageTest() {
		testNotEmpty(() -> {
			return ClassLoaders.retrieveLoadedClassesForPackage(Thread.currentThread().getContextClassLoader(), (pckg) -> pckg != null);
		});
	}
	
	@Test
	public void retrieveLoadedClassesTest() {
		testNotEmpty(() -> {
			return ClassLoaders.retrieveLoadedClasses(Thread.currentThread().getContextClassLoader());
		});
	}
	
	@Test
	public void retrieveLoadedAllClassesTest() {
		testNotEmpty(() -> {
			return ClassLoaders.retrieveAllLoadedClasses(Thread.currentThread().getContextClassLoader());
		});
	}
	
	@Test
	public void addClassPathsTestOne() {
		testNotNull(() -> {
			ComponentSupplier componentSupplier = getComponentSupplier();
			PathHelper pathHelper = componentSupplier.getPathHelper();
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			ClassLoaders.addClassPath(classLoader, pathHelper.getAbsolutePathOfResource("../../src/test/external-resources/commons-lang"));
			Class<?> cls = classLoader.loadClass("org.apache.commons.lang.ArrayUtils");
			return cls;
		});
	}
	
	@Test
	public void addClassPathsTestTwo() {
		testNotNull(() -> {
			ComponentSupplier componentSupplier = getComponentSupplier();
			PathHelper pathHelper = componentSupplier.getPathHelper();
			ClassLoader urlClassLoader = Thread.currentThread().getContextClassLoader();
			ClassLoaders.addClassPath(urlClassLoader, pathHelper.getAbsolutePathOfResource("../../src/test/external-resources/spring-core-4.3.4.RELEASE.jar"));
			Class<?> cls = urlClassLoader.loadClass("org.springframework.core.serializer.DefaultSerializer");
			return cls;
		});
	}
	
	@Test
	public void loadOrDefineByByteCodesTestOne() {
		testNotNull(() -> {
			try(MemoryClassLoader classLoader = getMemoryClassLoader(null)) {
				ComponentSupplier componentSupplier = getComponentSupplier();
				PathHelper pathHelper = componentSupplier.getPathHelper();
				try (SearchResult searchResult = componentSupplier.getByteCodeHunter().loadInCache(
					SearchConfig.forPaths(
							pathHelper.getAbsolutePathOfResource("../../src/test/external-resources/commons-lang")
					)
				).find();) {
					return ClassLoaders.loadOrDefineByByteCode(searchResult.getByteCodesFlatMap().get("org.apache.commons.lang.ArrayUtils"), classLoader);
				}
			}
		});
	}
	
	@Test
	public void loadOrDefineByByteCodesTestTwo() {
		testNotNull(() -> {
			try(MemoryClassLoader classLoader = getMemoryClassLoader(null)) {
				ComponentSupplier componentSupplier = getComponentSupplier();
				PathHelper pathHelper = componentSupplier.getPathHelper();
				try (SearchResult searchResult = componentSupplier.getByteCodeHunter().loadInCache(
					SearchConfig.forPaths(
							pathHelper.getAbsolutePathOfResource("../../src/test/external-resources/commons-lang")
					)
				).find();) {
					Map<String, ByteBuffer> byteCodesFound = searchResult.getByteCodesFlatMap();
					Map<String, JavaClass> byteCodes = new HashMap<>();
					return JavaClass.extractByUsing(byteCodesFound.get("org.apache.commons.lang.ArrayUtils"), (javaClass) -> {
						byteCodes.put("org.apache.commons.lang.ArrayUtils", javaClass);
						return ClassLoaders.loadOrDefineByJavaClass("org.apache.commons.lang.ArrayUtils", byteCodes, classLoader);
					});
				}
			}
		});
	}
	
	@Test
	public void createAndClose() {
		testDoesNotThrow(() -> {
			Classes.Loaders.create().close();
		});
	}

}
