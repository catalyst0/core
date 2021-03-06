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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.burningwave.core.Virtual;

@SuppressWarnings("unchecked")
class LoadOrBuildAndDefineConfigAbst<L extends LoadOrBuildAndDefineConfigAbst<L>> {
	
	Collection<UnitSourceGenerator> unitSourceGenerators;
	private Function<CompileConfig, CompileConfig> compileConfigSupplier;
	private Collection<String> classRepositoriesWhereToSearchNotFoundClassesDuringLoading;
	private Collection<String> additionalClassRepositoriesWhereToSearchNotFoundClassesDuringLoading;
	
	private ClassLoader classLoader;
	private boolean useOneShotJavaCompiler;
	private boolean virtualizeClasses;
		
	@SafeVarargs LoadOrBuildAndDefineConfigAbst(UnitSourceGenerator... unitsCode) {
		this(Arrays.asList(unitsCode));
	}
	
	@SafeVarargs
	LoadOrBuildAndDefineConfigAbst(Collection<UnitSourceGenerator>... unitCodeCollections) {
		virtualizeClasses = true;
		unitSourceGenerators = new HashSet<>();
		for (Collection<UnitSourceGenerator> unitsCode : unitCodeCollections) {
			unitSourceGenerators.addAll(unitsCode);
		}
		compileConfigSupplier = (compileConfig) -> {
			Collection<String> sources = new HashSet<>();
			for (UnitSourceGenerator unitCode : this.unitSourceGenerators) {
				unitCode.getAllClasses().entrySet().forEach(entry -> {
					if (virtualizeClasses) {
						entry.getValue().addConcretizedType(TypeDeclarationSourceGenerator.create(Virtual.class));
					}
				});
				sources.add(unitCode.make());
			}
			return CompileConfig.withSources(sources);
		};
	}
	
	public L virtualizeClasses(boolean flag) {
		this.virtualizeClasses = flag;
		return (L)this;
	}
	
	public L modifyCompileConfig(Consumer<CompileConfig> compileConfigModifier) {
		compileConfigSupplier = compileConfigSupplier.andThen((compileConfig) -> {
			compileConfigModifier.accept(compileConfig);
			return compileConfig;
		});
		return (L)this;
	}

////////////////////	
	
	@SafeVarargs
	public final L setClassRepository(String... classPaths) {
		return (L)setClassRepositories(Arrays.asList(classPaths));
	}
	
	@SafeVarargs
	public final L setClassRepositories(Collection<String>... classPathCollections) {
		modifyCompileConfig(compileConfig ->
			compileConfig.setClassRepositories(classPathCollections).neededClassesPreventiveSearch(true)
		);
		return (L)setClassRepositoriesWhereToSearchNotFoundClasses(classPathCollections);
	}
////////////////////	
	
	@SafeVarargs
	public final L addClassRepository(String... classPaths) {
		return (L)addClassRepositories(Arrays.asList(classPaths));
	}
	
	@SafeVarargs
	public final L addClassRepositories(Collection<String>... classPathCollections) {
		modifyCompileConfig(compileConfig ->
			compileConfig.addClassRepositories(classPathCollections).neededClassesPreventiveSearch(true)
		);
		return (L)addClassRepositoriesWhereToSearchNotFoundClasses(classPathCollections);
	}	
	
////////////////////
	
	@SafeVarargs
	public final L setClassPaths(String... classPaths) {
		return (L)setClassPaths(Arrays.asList(classPaths));
	}
	
	@SafeVarargs
	public final L setClassPaths(Collection<String>... classPathCollections) {
		modifyCompileConfig(compileConfig ->
			compileConfig.setClassPaths(classPathCollections)
		);
		return (L)setClassRepositoriesWhereToSearchNotFoundClasses(classPathCollections);
	}
////////////////////	
	
	@SafeVarargs
	public final L addClassPaths(String... classPaths) {
		return (L)addClassPaths(Arrays.asList(classPaths));
	}
	
	@SafeVarargs
	public final L addClassPaths(Collection<String>... classPathCollections) {
		modifyCompileConfig(compileConfig ->
			compileConfig.addClassPaths(classPathCollections)
		);
		return (L)addClassRepositoriesWhereToSearchNotFoundClasses(classPathCollections);
	}

////////////////////	
	
	@SafeVarargs
	public final L setClassRepositoryWhereToSearchNotFoundClasses(String... classPaths) {
		return setClassRepositoriesWhereToSearchNotFoundClasses(Arrays.asList(classPaths));		
	}
	
	@SafeVarargs
	public final L setClassRepositoriesWhereToSearchNotFoundClasses(Collection<String>... classPathCollections) {
		compileConfigSupplier = compileConfigSupplier.andThen((compileConfig) -> 
			compileConfig.setClassRepositoriesWhereToSearchNotFoundClasses(classPathCollections)
		);
		return setClassRepositoriesWhereToSearchNotFoundClassesDuringLoading(classPathCollections);		
	}

////////////////////	
	
	@SafeVarargs
	public final L addClassRepositoryWhereToSearchNotFoundClasses(String... classPaths) {
		return addClassRepositoriesWhereToSearchNotFoundClasses(Arrays.asList(classPaths));
	}
	
	@SafeVarargs
	public final L addClassRepositoriesWhereToSearchNotFoundClasses(Collection<String>... classPathCollections) {
		compileConfigSupplier = compileConfigSupplier.andThen((compileConfig) -> 
			compileConfig.addClassRepositoriesWhereToSearchNotFoundClasses(classPathCollections)
		);
		return addClassRepositoriesWhereToSearchNotFoundClassesDuringLoading(classPathCollections);		
	}

////////////////////
	
	@SafeVarargs
	public final L setClassRepositoryWhereToSearchNotFoundClassesDuringLoading(String... classPaths) {
		return (L)setClassRepositoriesWhereToSearchNotFoundClassesDuringLoading(Arrays.asList(classPaths));
	}
	
	@SafeVarargs
	public final L setClassRepositoriesWhereToSearchNotFoundClassesDuringLoading(Collection<String>... classPathCollections) {
		if (classRepositoriesWhereToSearchNotFoundClassesDuringLoading == null) {
			classRepositoriesWhereToSearchNotFoundClassesDuringLoading = new HashSet<>();
		}
		for (Collection<String> classPathCollection : classPathCollections) {
			classRepositoriesWhereToSearchNotFoundClassesDuringLoading.addAll(classPathCollection);
		}
		return (L)this;
	}

////////////////////	
	
	@SafeVarargs
	public final L addClassRepositoryWhereToSearchNotFoundClassesDuringLoading(String... classPaths) {
		return (L)addClassRepositoriesWhereToSearchNotFoundClassesDuringLoading(Arrays.asList(classPaths));
	}
	
	@SafeVarargs
	public final L addClassRepositoriesWhereToSearchNotFoundClassesDuringLoading(Collection<String>... classPathCollections) {
		if (additionalClassRepositoriesWhereToSearchNotFoundClassesDuringLoading == null) {
			additionalClassRepositoriesWhereToSearchNotFoundClassesDuringLoading = new HashSet<>();
		}
		for (Collection<String> classPathCollection : classPathCollections) {
			additionalClassRepositoriesWhereToSearchNotFoundClassesDuringLoading.addAll(classPathCollection);
		}
		return (L)this;
	}

////////////////////

	public L useClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
		return (L)this;
	}
	
	public L useOneShotJavaCompiler(boolean flag) {
		this.useOneShotJavaCompiler = flag;
		return (L)this;
	}

	Collection<String> getClassRepositoriesWhereToSearchNotFoundClassesDuringLoading() {
		return classRepositoriesWhereToSearchNotFoundClassesDuringLoading;
	}
	
	Collection<String> getAdditionalClassRepositoriesWhereToSearchNotFoundClassesDuringLoading() {
		return additionalClassRepositoriesWhereToSearchNotFoundClassesDuringLoading;
	}	
	
	ClassLoader getClassLoader() {
		return classLoader;
	}

	boolean isUseOneShotJavaCompilerEnabled() {
		return useOneShotJavaCompiler;
	}
	
	Collection<String> getClassesName() {
		Collection<String> classesName = new HashSet<>();
		unitSourceGenerators.stream().forEach(unitCode -> {
			unitCode.getAllClasses().entrySet().forEach(entry -> {
				classesName.add(entry.getKey());
			});
		});
		return classesName;
	}
	
	Supplier<CompileConfig> getCompileConfigSupplier() {
		return () -> compileConfigSupplier.apply(null);
	}
	
	boolean isVirtualizeClassesEnabled() {
		return virtualizeClasses;
	}
}