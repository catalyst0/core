/*
 * This file is part of Burningwave Core.
 *
 * Author: Roberto Gentili
 *
 * Hosted at: https://github.com/burningwave/core
 *
 * --
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Roberto Gentili
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without
 * limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO
 * EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
 * OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.burningwave.core.classes;

import static org.burningwave.core.assembler.StaticComponentContainer.ClassLoaders;
import static org.burningwave.core.assembler.StaticComponentContainer.IterableObjectHelper;
import static org.burningwave.core.assembler.StaticComponentContainer.SourceCodeHandler;
import static org.burningwave.core.assembler.StaticComponentContainer.Throwables;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.burningwave.core.Component;
import org.burningwave.core.assembler.ComponentSupplier;
import org.burningwave.core.classes.JavaMemoryCompiler.CompilationResult;
import org.burningwave.core.function.MultiParamsFunction;
import org.burningwave.core.function.ThrowingSupplier;
import org.burningwave.core.io.FileSystemItem;
import org.burningwave.core.io.PathHelper;
import org.burningwave.core.iterable.Properties;

@SuppressWarnings("unchecked")
public class ClassFactory implements Component {
	
	public static class Configuration {
		
		public static class Key {
			
			public static final String DEFAULT_CLASS_LOADER = "class-factory.default-class-loader";

			public static final String CLASS_REPOSITORIES_FOR_DEFAULT_CLASS_LOADER = PathHelper.Configuration.Key.PATHS_PREFIX + "class-factory.default-class-loader.class-repositories";
			public static final String ADDITIONAL_CLASS_REPOSITORIES_FOR_DEFAULT_CLASS_LOADER = PathHelper.Configuration.Key.PATHS_PREFIX + "class-factory.default-class-loader.additional-class-repositories";
			public static final String BYTE_CODE_HUNTER_SEARCH_CONFIG_CHECK_FILE_OPTIONS = "class-factory.byte-code-hunter.search-config.check-file-option";
					
		}
		
		public final static Map<String, Object> DEFAULT_VALUES;
	
		static {
			DEFAULT_VALUES = new HashMap<>();
			//DEFAULT_VALUES.put(Key.DEFAULT_CLASS_LOADER, Thread.currentThread().getContextClassLoader());
			DEFAULT_VALUES.put(Configuration.Key.DEFAULT_CLASS_LOADER + CodeExecutor.PROPERTIES_FILE_CODE_EXECUTOR_IMPORTS_KEY_SUFFIX,
				"${"+ Configuration.Key.DEFAULT_CLASS_LOADER + ".additional-imports}" +  ";" +
				ComponentSupplier.class.getName() + ";" +
				Function.class.getName() + ";" +
				FileSystemItem.class.getName() + ";" + 
				PathScannerClassLoader.class.getName() + ";" +
				Supplier.class.getName() + ";"
			);
			DEFAULT_VALUES.put(Configuration.Key.DEFAULT_CLASS_LOADER + CodeExecutor.PROPERTIES_FILE_CODE_EXECUTOR_NAME_KEY_SUFFIX, ClassFactory.class.getPackage().getName() + ".DefaultClassLoaderRetrieverForClassFactory");
			//DEFAULT_VALUES.put(Key.DEFAULT_CLASS_LOADER, "(Supplier<ClassLoader>)() -> ((ComponentSupplier)parameter[0]).getClassHunter().getPathScannerClassLoader()");
			DEFAULT_VALUES.put(
				Key.DEFAULT_CLASS_LOADER,
				(Function<ComponentSupplier, ClassLoader>)(componentSupplier) ->
					componentSupplier.getPathScannerClassLoader()
			);
			

			DEFAULT_VALUES.put(
				Key.CLASS_REPOSITORIES_FOR_DEFAULT_CLASS_LOADER,
				"${" + JavaMemoryCompiler.Configuration.Key.CLASS_PATHS + "}" + PathHelper.Configuration.Key.PATHS_SEPARATOR + 
				"${" + JavaMemoryCompiler.Configuration.Key.CLASS_REPOSITORIES + "}" + PathHelper.Configuration.Key.PATHS_SEPARATOR + 
				"${" + Key.ADDITIONAL_CLASS_REPOSITORIES_FOR_DEFAULT_CLASS_LOADER + "}"				
			);
			
			DEFAULT_VALUES.put(
				Key.BYTE_CODE_HUNTER_SEARCH_CONFIG_CHECK_FILE_OPTIONS,
				"${" + ClassPathScannerAbst.Configuration.Key.DEFAULT_CHECK_FILE_OPTIONS + "}"
			);
		}
	}
	
	
	private PathHelper pathHelper;
	private JavaMemoryCompiler javaMemoryCompiler;
	private PojoSubTypeRetriever pojoSubTypeRetriever;	
	private ClassLoader defaultClassLoader;
	private ByteCodeHunter byteCodeHunter;
	private ClassPathHunter classPathHunter;
	private Supplier<ClassPathHunter> classPathHunterSupplier;
	private Object defaultClassLoaderOrDefaultClassLoaderSupplier;
	private Supplier<ClassLoader> defaultClassLoaderSupplier;
	private Collection<ClassRetriever> classRetrievers;
	private Consumer<ClassLoader> classLoaderResetter;
	private Properties config;
	
	private ClassFactory(
		ByteCodeHunter byteCodeHunter,
		Supplier<ClassPathHunter> classPathHunterSupplier,
		JavaMemoryCompiler javaMemoryCompiler,
		PathHelper pathHelper,
		Object defaultClassLoaderOrDefaultClassLoaderSupplier,
		Consumer<ClassLoader> classLoaderResetter,
		Properties config
	) {	
		this.byteCodeHunter = byteCodeHunter;
		this.classPathHunterSupplier = classPathHunterSupplier;
		this.javaMemoryCompiler = javaMemoryCompiler;
		this.pathHelper = pathHelper;
		this.pojoSubTypeRetriever = PojoSubTypeRetriever.createDefault(this);
		this.defaultClassLoaderOrDefaultClassLoaderSupplier = defaultClassLoaderOrDefaultClassLoaderSupplier;
		this.classLoaderResetter = classLoaderResetter;
		this.classRetrievers = new CopyOnWriteArrayList<>();
		this.config = config;
		listenTo(config);
	}
	
	public static ClassFactory create(
		ByteCodeHunter byteCodeHunter,
		Supplier<ClassPathHunter> classPathHunterSupplier,
		JavaMemoryCompiler javaMemoryCompiler,
		PathHelper pathHelper,
		Object defaultClassLoaderSupplier,
		Consumer<ClassLoader> classLoaderResetter,
		Properties config
	) {
		return new ClassFactory(
			byteCodeHunter,
			classPathHunterSupplier,
			javaMemoryCompiler, 
			pathHelper,
			defaultClassLoaderSupplier,
			classLoaderResetter,
			config
		);
	}
	
	ClassLoader getDefaultClassLoader(Object client) {
		if (defaultClassLoaderSupplier != null) {
			ClassLoader classLoader = defaultClassLoaderSupplier.get();
			if (defaultClassLoader != classLoader) {
				synchronized(classLoader) {
					if (defaultClassLoader != classLoader) {
						ClassLoader oldClassLoader = this.defaultClassLoader;
						if (oldClassLoader != null && oldClassLoader instanceof MemoryClassLoader) {
							((MemoryClassLoader)oldClassLoader).unregister(this, true);
						}
						if (classLoader instanceof MemoryClassLoader) {
							if (!((MemoryClassLoader)classLoader).register(this)) {
								classLoader = getDefaultClassLoader(client);
							} else {
								((MemoryClassLoader)classLoader).register(client);
							}
						}
						this.defaultClassLoader = classLoader;
					}
				}
			}
			return classLoader;
		}
		if (defaultClassLoader == null) {
			synchronized (this) {
				if (defaultClassLoader == null) {
					Object classLoaderOrClassLoaderSupplier = ((Supplier<?>)this.defaultClassLoaderOrDefaultClassLoaderSupplier).get();
					if (classLoaderOrClassLoaderSupplier instanceof ClassLoader) {
						this.defaultClassLoader = (ClassLoader)classLoaderOrClassLoaderSupplier;
						if (defaultClassLoader instanceof MemoryClassLoader) {
							((MemoryClassLoader)defaultClassLoader).register(this);
							((MemoryClassLoader)defaultClassLoader).register(client);
						}
						return defaultClassLoader;
					} else if (classLoaderOrClassLoaderSupplier instanceof Supplier) {
						this.defaultClassLoaderSupplier = (Supplier<ClassLoader>) classLoaderOrClassLoaderSupplier;
						return getDefaultClassLoader(client);
					}
				} else { 
					return defaultClassLoader;
				}
			}
		}
		return defaultClassLoader;
	}
	
	private ClassPathHunter getClassPathHunter() {
		return classPathHunter != null? classPathHunter :
			(classPathHunter = classPathHunterSupplier.get());
	}
	
	public ClassRetriever loadOrBuildAndDefine(UnitSourceGenerator... unitsCode) {
		return loadOrBuildAndDefine(LoadOrBuildAndDefineConfig.forUnitSourceGenerator(unitsCode));
	}
	
	public <L extends LoadOrBuildAndDefineConfigAbst<L>> ClassRetriever loadOrBuildAndDefine(L config) {
		if (config.isVirtualizeClassesEnabled()) {
			config.addClassPaths(pathHelper.getBurningwaveRuntimeClassPath());
		}
		return loadOrBuildAndDefine(
			config.getClassesName(),
			config.getCompileConfigSupplier(),			
			config.isUseOneShotJavaCompilerEnabled(),
			IterableObjectHelper.merge(
				() -> config.getClassRepositoriesWhereToSearchNotFoundClassesDuringLoading(),
				() -> config.getAdditionalClassRepositoriesWhereToSearchNotFoundClassesDuringLoading(),
				() -> {
					Collection<String> classRepositoriesForNotFoundClasses = pathHelper.getPaths(
						Configuration.Key.CLASS_REPOSITORIES_FOR_DEFAULT_CLASS_LOADER, 
						Configuration.Key.ADDITIONAL_CLASS_REPOSITORIES_FOR_DEFAULT_CLASS_LOADER
					);
					if (!classRepositoriesForNotFoundClasses.isEmpty()) {
						config.addClassRepositoriesWhereToSearchNotFoundClasses(classRepositoriesForNotFoundClasses);
					}
					return classRepositoriesForNotFoundClasses;
				}
			),
			(client) -> Optional.ofNullable(
				config.getClassLoader()
			).orElseGet(() -> 
				getDefaultClassLoader(client)
			)
		);
	}
	
	private ClassRetriever loadOrBuildAndDefine(
		Collection<String> classesName,
		Supplier<CompileConfig> compileConfigSupplier,		
		boolean useOneShotJavaCompiler,
		Collection<String> classPathsForNotFoundClassesDuringLoading,
		Function<Object, ClassLoader> classLoaderSupplier
	) {
		try {
			Object temporaryClient = new Object();
			ClassLoader classLoader = classLoaderSupplier.apply(temporaryClient);
			Function<ClassRetriever, ClassLoader> classLoaderSupplierForClassRetriever = (classRetriever) -> {
				if (classLoader instanceof MemoryClassLoader) {
					((MemoryClassLoader)classLoader).register(classRetriever);
					((MemoryClassLoader)classLoader).unregister(temporaryClient, true);
				}
				return classLoader;
			};
			if (classLoader instanceof PathScannerClassLoader) {
				((PathScannerClassLoader)classLoader).scanPathsAndAddAllByteCodesFound(
					classPathsForNotFoundClassesDuringLoading
				);
			}
			Map<String, Class<?>> classes = new HashMap<>();
			AtomicReference<Map<String, ByteBuffer>> retrievedBytecodes = new AtomicReference<>();
			for (String className : classesName) {
				try {
					classes.put(className, classLoader.loadClass(className));
				} catch (Throwable exc) {
					CompilationResult compilationResult = build0(
						compileConfigSupplier.get(),
						useOneShotJavaCompiler
					);
					logInfo(
						classesName.size() > 1?	
							"Classes {} have been succesfully compiled":
							"Class {} has been succesfully compiled",
						classesName.size() > 1?		
							String.join(", ", classesName):
							classesName.stream().findFirst().orElseGet(() -> "")
					);
					if (classLoader instanceof PathScannerClassLoader) {
						((PathScannerClassLoader)classLoader).scanPathsAndAddAllByteCodesFound(
							Arrays.asList(compilationResult.getClassPath().getAbsolutePath()), true
						);
					}
					return new ClassRetriever(this, classLoaderSupplierForClassRetriever) {
						@Override
						public Class<?> get(Map<String, ByteBuffer> additionalByteCodes, String className) {
							try {
								Map<String, ByteBuffer> finalByteCodes = compilationResult.getCompiledFiles();
								if (additionalByteCodes != null) {
									finalByteCodes = new HashMap<>(compilationResult.getCompiledFiles());
									finalByteCodes.putAll(additionalByteCodes);
								}
								if (classLoader instanceof PathScannerClassLoader) {
									return classLoader.loadClass(className);
								} else {
									return ClassLoaders.loadOrDefineByByteCode(className, finalByteCodes, classLoader);
								}
							} catch (Throwable innExc) {
								return ThrowingSupplier.get(() -> {
									return ClassLoaders.loadOrDefineByByteCode(className, 
										loadBytecodesFromClassPaths(
											retrievedBytecodes,
											classPathsForNotFoundClassesDuringLoading,
											compilationResult.getCompiledFiles(),
											additionalByteCodes
										).get(), classLoader
									);
								});
							}
						}
					};
					
				}
			}
			logInfo("Classes {} loaded by classloader {} without building", String.join(", ", classes.keySet()), classLoader);
			return new ClassRetriever(this, classLoaderSupplierForClassRetriever) {
				@Override
				public Class<?> get(Map<String, ByteBuffer> additionalByteCodes, String className) {
					try {
						return classLoader.loadClass(className);
					} catch (Throwable exc) {
						try {
							return ClassLoaders.loadOrDefineByByteCode(className, Optional.ofNullable(additionalByteCodes).orElseGet(HashMap::new), classLoader);
						} catch (Throwable exc2) {
							return ThrowingSupplier.get(() -> 
								ClassLoaders.loadOrDefineByByteCode(
									className,
									loadBytecodesFromClassPaths(
										retrievedBytecodes, 
										classPathsForNotFoundClassesDuringLoading,
										additionalByteCodes
									).get(), 
									classLoader
								)
							);
						}
					}
				}
			};
		} catch (Throwable exc) {
			throw Throwables.toRuntimeException(exc);
		}
	}
	
	@SafeVarargs
	private final AtomicReference<Map<String, ByteBuffer>> loadBytecodesFromClassPaths(
		AtomicReference<Map<String, ByteBuffer>> retrievedBytecodes,
		Collection<String> classPaths,
		Map<String, ByteBuffer>... extraBytecodes
	) {
		if (retrievedBytecodes.get() == null) {
			synchronized (retrievedBytecodes) {
				if (retrievedBytecodes.get() == null) {
					try(ByteCodeHunter.SearchResult result = byteCodeHunter.loadInCache(
						SearchConfig.forPaths(
							classPaths
						).deleteFoundItemsOnClose(
							false
						).withScanFileCriteria(
							FileSystemItem.Criteria.forClassTypeFiles(
								config.resolveStringValue(
									Configuration.Key.BYTE_CODE_HUNTER_SEARCH_CONFIG_CHECK_FILE_OPTIONS,
									Configuration.DEFAULT_VALUES
								)
							)
						).optimizePaths(
							true
						)
					).find()) {
						Map<String, ByteBuffer> extraClassPathsForClassLoaderByteCodes = new HashMap<>();
						result.getItemsFoundFlatMap().values().forEach(javaClass -> {
							extraClassPathsForClassLoaderByteCodes.put(javaClass.getName(), javaClass.getByteCode());
						});
						retrievedBytecodes.set(extraClassPathsForClassLoaderByteCodes);
					}
				}
			}
		}
		if (extraBytecodes != null && extraBytecodes.length > 0) {
			for (Map<String, ByteBuffer> extraBytecode : extraBytecodes) {
				if (extraBytecode != null) {
					synchronized(retrievedBytecodes) {
						retrievedBytecodes.get().putAll(extraBytecode);
					}
				}
			}
		}
		return retrievedBytecodes;
	}
	
	private CompilationResult build0(
		CompileConfig compileConfig,
		boolean useOneShotCompiler
		
	) {
		if (useOneShotCompiler) {
			try (JavaMemoryCompiler compiler = JavaMemoryCompiler.create(
				pathHelper,
				getClassPathHunter(),
				config
			)) {
				return compiler.compile(
					compileConfig
				);
			}
		} else {
			return this.javaMemoryCompiler.compile(
				compileConfig
			);
		}
	}
	
	public PojoSubTypeRetriever createPojoSubTypeRetriever(PojoSourceGenerator sourceGenerator) {
		return PojoSubTypeRetriever.create(this, sourceGenerator);
	}
	
	public <T> Class<T> loadOrBuildAndDefinePojoSubType(String className, Class<?>... superClasses) {
		return loadOrBuildAndDefinePojoSubType(null, className, superClasses);
	}
	
	public <T> Class<T> loadOrBuildAndDefinePojoSubType(String className, int options, Class<?>... superClasses) {
		return loadOrBuildAndDefinePojoSubType(null, className, options, superClasses);
	}
	
	public <T> Class<T> loadOrBuildAndDefinePojoSubType(ClassLoader classLoader, String className, int options, Class<?>... superClasses) {
		return pojoSubTypeRetriever.loadOrBuildAndDefine(classLoader, className, options, superClasses);
	}
	
	public <T> Class<T> loadOrBuildAndDefinePojoSubType(ClassLoader classLoader, String className, Class<?>... superClasses) {
		return pojoSubTypeRetriever.loadOrBuildAndDefine(classLoader, className, PojoSourceGenerator.ALL_OPTIONS_DISABLED, superClasses);
	}
	
	public <T> Class<T> loadOrBuildAndDefineFunctionSubType(int parametersCount) {
		return loadOrBuildAndDefineFunctionSubType(null, parametersCount);
	}
	
	public <T> Class<T> loadOrBuildAndDefineFunctionSubType(ClassLoader classLoader, int parametersLength) {
		return loadOrBuildAndDefineFunctionInterfaceSubType(
			classLoader, "FunctionFor", "Parameters", parametersLength,
			(className, paramsL) -> SourceCodeHandler.generateFunction(className, paramsL)
		);
	}
	
	public <T> Class<T> loadOrBuildAndDefineConsumerSubType(int parametersCount) {
		return loadOrBuildAndDefineConsumerSubType(null, parametersCount);
	}
	
	public <T> Class<T> loadOrBuildAndDefineConsumerSubType(ClassLoader classLoader, int parametersLength) {
		return loadOrBuildAndDefineFunctionInterfaceSubType(
			classLoader, "ConsumerFor", "Parameters", parametersLength,
			(className, paramsL) -> SourceCodeHandler.generateConsumer(className, paramsL)
		);
	}
	
	public <T> Class<T> loadOrBuildAndDefinePredicateSubType(int parametersLength) {
		return loadOrBuildAndDefinePredicateSubType(null, parametersLength);
	}
	
	public <T> Class<T> loadOrBuildAndDefinePredicateSubType(ClassLoader classLoader, int parametersLength) {
		return loadOrBuildAndDefineFunctionInterfaceSubType(
			classLoader, "PredicateFor", "Parameters", parametersLength,
			(className, paramsL) -> SourceCodeHandler.generatePredicate(className, paramsL)
		);
	}
	
	private <T> Class<T> loadOrBuildAndDefineFunctionInterfaceSubType(
		ClassLoader classLoader,
		String classNamePrefix, 
		String classNameSuffix,
		int parametersLength,
		BiFunction<String, Integer, UnitSourceGenerator> unitSourceGeneratorSupplier
	) {
		String functionalInterfaceName = classNamePrefix + parametersLength +	classNameSuffix;
		String packageName = MultiParamsFunction.class.getPackage().getName();
		String className = packageName + "." + functionalInterfaceName;
		ClassRetriever classRetriever = loadOrBuildAndDefine(
			LoadOrBuildAndDefineConfig.forUnitSourceGenerator(
				unitSourceGeneratorSupplier.apply(className, parametersLength)
			).useClassLoader(
				classLoader
			)
		);
		Class<T> cls = (Class<T>)classRetriever.get(className);
		classRetriever.close();
		return cls;
	}
	
	
	
	public static class PojoSubTypeRetriever {
		private ClassFactory classFactory;
		private PojoSourceGenerator sourceGenerator;
		
		private PojoSubTypeRetriever(
			ClassFactory classFactory,
			PojoSourceGenerator sourceGenerator
		) {
			this.classFactory = classFactory;
			this.sourceGenerator = sourceGenerator;
		}
		
		public static PojoSubTypeRetriever create(ClassFactory classFactory, PojoSourceGenerator sourceGenerator) {
			return new PojoSubTypeRetriever(classFactory, sourceGenerator) ;
		}

		public static PojoSubTypeRetriever createDefault(ClassFactory classFactory) {
			return new PojoSubTypeRetriever(classFactory, PojoSourceGenerator.createDefault());
		}
		
		public <T> Class<T> getOrBuild(
				ClassLoader classLoader,
			String className,
			Class<?>... superClasses
		) {
			return loadOrBuildAndDefine(classLoader, className, PojoSourceGenerator.ALL_OPTIONS_DISABLED, superClasses);
		}	
		
		public <T> Class<T> loadOrBuildAndDefine(
			String className,
			int options, 
			Class<?>... superClasses
		) {	
			ClassRetriever classRetriever = classFactory.loadOrBuildAndDefine(
				LoadOrBuildAndDefineConfig.forUnitSourceGenerator(
					sourceGenerator.create(className, options, superClasses)
				)
			);
			Class<T> cls = (Class<T>)classRetriever.get(className);
			classRetriever.close();
			return cls;
		}
		
		public <T> Class<T> loadOrBuildAndDefine(
			ClassLoader classLoader,
			String className,
			int options, 
			Class<?>... superClasses
		) {	
			ClassRetriever classRetriever = classFactory.loadOrBuildAndDefine(
				LoadOrBuildAndDefineConfig.forUnitSourceGenerator(
					sourceGenerator.create(className, options, superClasses)
				).useClassLoader(classLoader)
			);
			Class<T> cls = (Class<T>)classRetriever.get(className);
			classRetriever.close();
			return cls;
		}
			
	}
	
	boolean register(ClassRetriever classRetriever) {
		classRetrievers.add(classRetriever);
		return true;
	}
	
	boolean unregister(ClassRetriever classRetriever) {
		classRetrievers.remove(classRetriever);
		return true;
	}
	
	public synchronized void closeClassRetrievers() {
		Collection<ClassRetriever> classRetrievers = this.classRetrievers;
		if (classRetrievers != null) {
			Iterator<ClassRetriever> classRetrieverIterator = classRetrievers.iterator();		
			while(classRetrieverIterator.hasNext()) {
				ClassRetriever classRetriever = classRetrieverIterator.next();
				classRetriever.close();
			}
		}
	}
	
	public void reset(boolean closeClassRetrievers) {
		if (closeClassRetrievers) {
			closeClassRetrievers();
		}
		ClassLoader defaultClassLoader = this.defaultClassLoader;
		if (defaultClassLoader != null) {
			this.defaultClassLoader = null;
			classLoaderResetter.accept(defaultClassLoader);
			if (defaultClassLoader instanceof MemoryClassLoader) {
				((MemoryClassLoader)defaultClassLoader).unregister(this, true);
			}
		}		
	}
	
	@Override
	public void close() {
		unregister(config);
		closeClassRetrievers();
		this.classRetrievers = null;
		pathHelper = null;
		javaMemoryCompiler = null;
		pojoSubTypeRetriever = null;	
		if (defaultClassLoader instanceof MemoryClassLoader) {
			((MemoryClassLoader)defaultClassLoader).unregister(this, true);
		}
		defaultClassLoader = null;
		byteCodeHunter = null;
		classPathHunter = null;
		classPathHunterSupplier = null;
		defaultClassLoaderOrDefaultClassLoaderSupplier = null;
		defaultClassLoaderOrDefaultClassLoaderSupplier = null;
		defaultClassLoaderSupplier = null;
		classLoaderResetter = null;		
		config = null;
	}

	public static abstract class ClassRetriever implements Component {
		private ClassLoader classLoader;
		private ClassFactory classFactory;
		
		private ClassRetriever(ClassFactory classFactory, Function<ClassRetriever, ClassLoader> classLoaderSupplier) {
			this.classLoader = classLoaderSupplier.apply(this);
			this.classFactory = classFactory;
			this.classFactory.register(this);
		}
		
		public abstract Class<?> get(Map<String, ByteBuffer> additionalByteCodes, String className);
		
		public Collection<Class<?>> get(Map<String, ByteBuffer> additionalByteCodes, String... classNames) {
			Collection<Class<?>> classes = new HashSet<>();
			for(String className : classNames) {
				classes.add(get(additionalByteCodes, className));
			}
			return classes;
		}
		
		public Class<?> get(String className) {
			return get(null, className);
		}
		
		public Collection<Class<?>> get(String... classesName) {
			Collection<Class<?>> classes = new HashSet<>();
			for(String className : classesName) {
				classes.add(get(null, className));
			}
			return classes;
		}
		
		@Override
		public void close() {
			if (classLoader instanceof MemoryClassLoader) {
				((MemoryClassLoader)classLoader).unregister(this, true);
			}
			this.classLoader = null;
			this.classFactory.unregister(this);
			classFactory = null;
		}
	}
}
