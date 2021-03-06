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
import static org.burningwave.core.assembler.StaticComponentContainer.ClassLoaders;
import static org.burningwave.core.assembler.StaticComponentContainer.Classes;
import static org.burningwave.core.assembler.StaticComponentContainer.Strings;
import static org.burningwave.core.assembler.StaticComponentContainer.Throwables;

import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.ProtectionDomain;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import org.burningwave.core.Component;
import org.burningwave.core.io.ByteBufferInputStream;


@SuppressWarnings("unchecked")
public class MemoryClassLoader extends ClassLoader implements Component {

	Map<String, ByteBuffer> notLoadedByteCodes;
	Map<String, ByteBuffer> loadedByteCodes;
	HashSet<Object> clients;
	boolean isClosed;
	
	static {
        ClassLoader.registerAsParallelCapable();
    }
	
	protected MemoryClassLoader(
		ClassLoader parentClassLoader
	) {
		super(parentClassLoader);
		if (parentClassLoader instanceof MemoryClassLoader) {
			((MemoryClassLoader)parentClassLoader).register(this);
		}
		this.notLoadedByteCodes = new HashMap<>();
		this.loadedByteCodes = new HashMap<>();
		this.clients = new HashSet<>();
	}
	
	public static MemoryClassLoader create(ClassLoader parentClassLoader) {
		return new MemoryClassLoader(parentClassLoader);
	}

	public void addByteCode(String className, ByteBuffer byteCode) {
    	try {
			addByteCode0(className, byteCode);
    	} catch (Throwable exc) {
    		if (!isClosed) {
    			throw exc;
    		} else {
    			logWarn("Could not execute addByteCode on class named {} because {} has been closed", className, this.toString());
    		}
    	}
    }

	void addByteCode0(String className, ByteBuffer byteCode) {
		if (ClassLoaders.retrieveLoadedClass(this, className) == null) {
			synchronized (notLoadedByteCodes) {
				notLoadedByteCodes.put(className, byteCode);
			}
		} else {
			logWarn("Could not add compiled class {} cause it's already defined", className);
		}
	}
    
    public Map.Entry<String, ByteBuffer> getNotLoadedByteCode(String className) {
    	try {
        	for (Map.Entry<String, ByteBuffer> entry : notLoadedByteCodes.entrySet()){
        	    if (entry.getKey().equals(className)) {
        	    	return entry;
        	    }
        	}
    	} catch (Throwable exc) {
    		if (!isClosed) {
    			throw exc;
    		} else {
    			logWarn("Could not execute getNotLoadedByteCode on class named {} because {} has been closed", className, this.toString());
    		}
    	}
    	return null;
    }
    
    public ByteBuffer getByteCodeOf(String className) {
    	try {
    		return Optional.ofNullable(notLoadedByteCodes.get(className)).orElseGet(() -> Optional.ofNullable(loadedByteCodes.get(className)).orElseGet(() -> null));
    	} catch (Throwable exc) {
    		if (!isClosed) {
    			throw exc;
    		} else {
    			logWarn("Could not execute getByteCodeOf on class named {} because {} has been closed", className, this.toString());
    		}
    	}
    	return null;
    }
    
    void addByteCodes(Map<String, ByteBuffer> byteCodes) {
    	try {
    		for (Map.Entry<String, ByteBuffer> clazz : byteCodes.entrySet()) {
    			addByteCode0(
    				clazz.getKey(), clazz.getValue()
    			);
    		}
    	} catch (Throwable exc) {
    		if (!isClosed) {
    			throw exc;
    		} else {
    			logWarn("Could not execute addByteCodes on {} because {} has been closed", byteCodes.toString(), this.toString());
    		}
    	}
		
    }
    
    public void addByteCodes(Collection<Entry<String, ByteBuffer>> classes) {
    	try {
    		for (Map.Entry<String, ByteBuffer> clazz : classes) {
    			addByteCode0(
    				clazz.getKey(), clazz.getValue()
    			);
    		}
    	} catch (Throwable exc) {
    		if (!isClosed) {
    			throw exc;
    		} else {
    			logWarn("Could not execute addByteCodes on {} because {} has been closed", classes.toString(), this.toString());
    		}
    	}
	} 

	public void addByteCodes(Entry<String, ByteBuffer>... classes) {
		try {
    		for (Map.Entry<String, ByteBuffer> clazz : classes) {
    			addByteCode0(
    				clazz.getKey(), clazz.getValue()
    			);
    		}
    	} catch (Throwable exc) {
    		if (!isClosed) {
    			throw exc;
    		} else {
    			logWarn("Could not execute addByteCodes on {} because {} has been closed", classes.toString(), this.toString());
    		}
    	}    	
	} 
    
	public boolean hasPackageBeenDefined(String packageName) {
		return Strings.isEmpty(packageName) || ClassLoaders.retrieveLoadedPackage(this, packageName) != null;
	}
    
    @Override
    protected Package definePackage(String packageName, String specTitle,
		String specVersion, String specVendor, String implTitle,
		String implVersion, String implVendor, URL sealBase
	) throws IllegalArgumentException {
    	Package pkg = null;
    	if (Strings.isNotEmpty(packageName)) {
    		pkg = ClassLoaders.retrieveLoadedPackage(this, packageName);
    		if (pkg == null) {
    			try {
    				pkg = super.definePackage(packageName, specTitle, specVersion, specVendor, implTitle,
    		    			implVersion, implVendor, sealBase);
    			} catch (IllegalArgumentException exc) {
    				logWarn("Package " + packageName + " already defined");
    				pkg = ClassLoaders.retrieveLoadedPackage(this, packageName);
    			}
    		}
    	}
    	return pkg;
    }
    
	void definePackageOf(Class<?> cls) {
		if (cls.getName().contains(".")) {
			String pckgName = cls.getName().substring(
		    	0, cls.getName().lastIndexOf(".")
		    );
		    if (ClassLoaders.retrieveLoadedPackage(this, pckgName) == null) {
		    	definePackage(pckgName, null, null, null, null, null, null, null);
			}	
		}
	}
    
    @Override
    protected Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {
    	Class<?> cls = null;
    	try {
			cls = super.loadClass(className, resolve);
		} catch (SecurityException exc) {
			cls = Class.forName(className);
		}
    	removeNotLoadedCompiledClass(className);
    	return cls;
    }
    
    
    public Class<?> loadOrDefineClass(Class<?> toLoad) throws ClassNotFoundException {
    	return ClassLoaders.loadOrDefine(toLoad, this);
    }
    
    public Class<?> loadOrDefineClass(JavaClass toLoad) throws ClassNotFoundException {
    	return ClassLoaders.loadOrDefineByJavaClass(toLoad, this);
    }
    
    public Class<?> loadOrDefineClass(ByteBuffer byteCode) throws ClassNotFoundException {
    	return ClassLoaders.loadOrDefineByByteCode(byteCode, this);
    }
    
    
    @Override
    public InputStream getResourceAsStream(String name) {
    	ClassLoader parentClassLoader = getParent();
    	InputStream inputStream = null;
    	if (parentClassLoader != null) {
    		inputStream = parentClassLoader.getResourceAsStream(name);
    	}
    	if (inputStream == null && name.endsWith(".class")) {
    		inputStream = getByteCodeAsInputStream(name);
    	}
    	return inputStream;
    }
    
	protected InputStream getByteCodeAsInputStream(String classRelativePath) {
		if (classRelativePath.endsWith(".class")) {
			ByteBuffer byteCode = getByteCode(classRelativePath);
    		if (byteCode != null) {
	    		return new ByteBufferInputStream(
	    			byteCode
	    		);
    		}
    	}
		return null;
	}
    
	ByteBuffer getByteCode(String classRelativePath) {
		try {
			String className = classRelativePath.substring(0, classRelativePath.lastIndexOf(".class")).replace("/", ".");
			ByteBuffer byteCode = loadedByteCodes.get(className);
			if (byteCode == null) {
				byteCode = notLoadedByteCodes.get(className);
			}
			return byteCode;
    	} catch (Throwable exc) {
    		if (!isClosed) {
    			throw exc;
    		} else {
    			logWarn("Could not execute getByteCode on {} because {} has been closed", classRelativePath, this.toString());
    		}
    	}    
		return null;
	}
    
    
    protected void addLoadedByteCode(String className, ByteBuffer byteCode) {
    	try {
    		synchronized (loadedByteCodes) {
        		loadedByteCodes.put(className, byteCode);
    		}
    	} catch (Throwable exc) {
    		if (!isClosed) {
    			throw exc;
    		} else {
    			logWarn("Could not execute addLoadedByteCode on {} because {} has been closed", className, this.toString());
    		}
    	}    
    }
    
    
	@Override
    protected Class<?> findClass(String className) throws ClassNotFoundException {
		Class<?> cls = null;
		try {
			ByteBuffer byteCode = notLoadedByteCodes.get(className);
			if (byteCode != null) {
				try {
					cls = _defineClass(className, byteCode, null);
	        		definePackageOf(cls);
	        	} catch (NoClassDefFoundError exc) {
	        		String notFoundClassName = Classes.retrieveName(exc);
	        		removeNotLoadedCompiledClass(className);
					logWarn("Could not load compiled class " + className + " because class " + notFoundClassName + 
						" could not be found, so it will be removed: " + exc.toString()
					);
	    			throw exc;
	        	}
			} else {
				logWarn("Compiled class " + className + " not found");
			}
		} catch (Throwable exc) {
			if (isClosed) {
				logWarn("Could not load class {} because {} has been closed", className, this.toString());
			} else {
				throw exc;
			}
		}
		if (cls != null) {
			return cls;
		} else {
			throw new ClassNotFoundException(className);
		}
	}
	
	Class<?> _defineClass(String className, java.nio.ByteBuffer byteCode, ProtectionDomain protectionDomain) {
		Class<?> cls = super.defineClass(className, byteCode, protectionDomain);
		addLoadedByteCode(className, byteCode);
		removeNotLoadedCompiledClass(className);
		return cls;
	}

	public void removeNotLoadedCompiledClass(String className) {
		try {
			synchronized (notLoadedByteCodes) {
				notLoadedByteCodes.remove(className);
			}
    	} catch (Throwable exc) {
    		if (!isClosed) {
    			throw exc;
    		} else {
    			logWarn("Could not execute removeNotLoadedCompiledClass on class named {} because {} has been closed", className, this.toString());
    		}
    	}    
	}
	
	
	public Set<Class<?>> getLoadedClassesForPackage(Predicate<Package> packagePredicate	) {
		return ClassLoaders.retrieveLoadedClassesForPackage(this, packagePredicate);
	}
	
	Map<String, ByteBuffer> getLoadedCompiledClasses() {
		return loadedByteCodes;
	}
		
	public Collection<Class<?>> forceCompiledClassesLoading() {
		Collection<Class<?>> loadedClasses = new HashSet<>();
		for (Map.Entry<String, ByteBuffer> entry : new HashMap<>(notLoadedByteCodes).entrySet()){
			try {
				loadedClasses.add(loadClass(entry.getKey()));
			} catch (Throwable exc) {
				logWarn("Could not load class " + entry.getKey(), exc.getMessage());
			}
		}
		return loadedClasses;
	}
	
	@Override
	public MemoryClassLoader clear () {
		try {
			this.notLoadedByteCodes.clear();
			this.loadedByteCodes.clear();
    	} catch (Throwable exc) {
    		if (!isClosed) {
    			throw exc;
    		} else {
    			logWarn("Could not execute clear because {} has been closed", this.toString());
    		}
    	}  
		return this;
	}
	
	protected void unregister() {
		ClassLoaders.unregister(this);
		Cache.classLoaderForConstructors.remove(this);
		Cache.classLoaderForFields.remove(this);
		Cache.classLoaderForMethods.remove(this);
		Cache.uniqueKeyForFields.remove(this);
		Cache.uniqueKeyForConstructors.remove(this);
		Cache.uniqueKeyForMethods.remove(this);
		Cache.bindedFunctionalInterfaces.remove(this);
		Cache.uniqueKeyForExecutableAndMethodHandle.remove(this);
	}
	
	public synchronized boolean register(Object client) {
		HashSet<Object> clients = this.clients;
		if (!isClosed) {
			clients.add(client);
			return true;
		}
		return false;
	}
	
	public synchronized boolean unregister(Object client, boolean close) {
		HashSet<Object> clients = this.clients;
		if (!isClosed) {
			clients.remove(client);
			if (clients.isEmpty() && close) {
				close();
				return true;
			}
		}
		return false;
	}
	
	public synchronized void close() {
		HashSet<Object> clients = this.clients;
		if (clients != null && !clients.isEmpty()) {
			throw Throwables.toRuntimeException("Could not close " + this + " because there are " + clients.size() +" registered clients");
		}
		isClosed = true;
		ClassLoader parentClassLoader = getParent();
		if (parentClassLoader != null && parentClassLoader instanceof MemoryClassLoader) {
			((MemoryClassLoader)parentClassLoader).unregister(this,true);
		}
		if (clients != null) {
			clients.clear();
		}
		this.clients = null;
		clear();
		notLoadedByteCodes = null;
		loadedByteCodes = null;
		Collection<Class<?>> loadedClasses = ClassLoaders.retrieveLoadedClasses(this);
		if (loadedClasses != null) {
			loadedClasses.clear();
		}
		unregister();
	}
}