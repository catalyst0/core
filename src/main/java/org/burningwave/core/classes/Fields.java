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

import static org.burningwave.core.assembler.StaticComponentContainer.Cache;
import static org.burningwave.core.assembler.StaticComponentContainer.Classes;
import static org.burningwave.core.assembler.StaticComponentContainer.LowLevelObjectsHandler;
import static org.burningwave.core.assembler.StaticComponentContainer.Throwables;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.stream.Collectors;

import org.burningwave.core.function.ThrowingRunnable;
import org.burningwave.core.function.ThrowingSupplier;

@SuppressWarnings("unchecked")
public class Fields extends MemberHelper<Field> {
	
	public static Fields create() {
		return new Fields();
	}
	
	public <T> T get(Object target, Field field) {
		return ThrowingSupplier.get(() -> (T)field.get(target));
	}
	
	public <T> T get(Object target, String fieldName) {
		return get(target, findFirstAndMakeItAccessible(Classes.retrieveFrom(target), fieldName));
	}	
	
	public <T> T getDirect(Object target, Field field) {
		return ThrowingSupplier.get(() -> (T)LowLevelObjectsHandler.getFieldValue(target, field));
	}
	
	public <T> T getDirect(Object target, String fieldName) {
		return getDirect(target, findFirstAndMakeItAccessible(Classes.retrieveFrom(target), fieldName));
	}
	
	public void set(Object target, String fieldName, Object value) {
		set(target, findFirstAndMakeItAccessible(Classes.retrieveFrom(target), fieldName), value);
	}
	
	public void set(Object target, Field field, Object value) {
		ThrowingRunnable.run(() -> field.set(target, value));
	}
	
	public void setDirect(Object target, String fieldName, Object value) {
		setDirect(target, findFirstAndMakeItAccessible(Classes.retrieveFrom(target), fieldName), value);
	}
	
	public void setDirect(Object target, Field field, Object value) {
		LowLevelObjectsHandler.setFieldValue(target, field, value);
	}
	
	public Map<Field, ?> getAll(Object target) {
		Map<Field, Object> fieldValues = new HashMap<>();
		Collection<Field> fields = findAllAndMakeThemAccessible(Classes.deepRetrieveFrom(target));
		for (Field field : fields) {
			fieldValues.put(
				field,
				ThrowingSupplier.get(
					() ->
						field.get(
							Modifier.isStatic(field.getModifiers()) ? null : target
						)
				)
			);
		}
		return fieldValues;
	}
	
	public Map<Field, ?> getAllDirect(Object target) {
		Map<Field, ?> fieldValues = new HashMap<>();
		Collection<Field> fields = findAllAndMakeThemAccessible(Classes.deepRetrieveFrom(target));
		for (Field field : fields) {
			fieldValues.put(
				field,
				ThrowingSupplier.get(() -> LowLevelObjectsHandler.getFieldValue(target, field))
			);
		}
		return fieldValues;
	}
	
	public Field findOneAndMakeItAccessible(Class<?> targetClass, String memberName) {
		Collection<Field> members = findAllByExactNameAndMakeThemAccessible(targetClass, memberName);
		if (members.size() != 1) {
			throw Throwables.toRuntimeException("Field " + memberName
				+ " not found or found more than one field in " + targetClass.getName()
				+ " hierarchy");
		}
		return members.stream().findFirst().get();
	}
	
	public Field findFirstAndMakeItAccessible(Class<?> targetClass, String fieldName) {
		Collection<Field> members = findAllByExactNameAndMakeThemAccessible(targetClass, fieldName);
		if (members.size() < 1) {
			throw Throwables.toRuntimeException("Field " + fieldName
				+ " not found in " + targetClass.getName()
				+ " hierarchy");
		}
		return members.stream().findFirst().get();
	}

	public Collection<Field> findAllByExactNameAndMakeThemAccessible(
		Class<?> targetClass,
		String fieldName
	) {	
		String cacheKey = getCacheKey(targetClass, "equals " + fieldName, (Class<?>[])null);
		ClassLoader targetClassClassLoader = Classes.getClassLoader(targetClass);
		return Cache.uniqueKeyForFields.getOrUploadIfAbsent(
			targetClassClassLoader,
			cacheKey, 
			() -> 
				Collections.unmodifiableCollection(
					findAllAndMakeThemAccessible(targetClass).stream().filter(field -> field.getName().equals(fieldName)).collect(Collectors.toCollection(LinkedHashSet::new))
				)
		);
	}
	
	public Collection<Field> findAllAndMakeThemAccessible(
		Class<?> targetClass
	) {	
		String cacheKey = getCacheKey(targetClass, "all fields", (Class<?>[])null);
		ClassLoader targetClassClassLoader = Classes.getClassLoader(targetClass);
		return Cache.uniqueKeyForFields.getOrUploadIfAbsent(
			targetClassClassLoader, 
			cacheKey, 
			() -> 
				Collections.unmodifiableCollection(
					findAllAndApply(
						FieldCriteria.create(),
						targetClass,
						(field) -> 
							field.setAccessible(true)
					)
				)
			
		);
	}	
}
