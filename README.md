# Burningwave Core [![Tweet](https://img.shields.io/twitter/url/http/shields.io.svg?style=social)](https://twitter.com/intent/tweet?text=%40Burningwave_fw%20Core%2C%20the%20%23Java%20frameworks%20building%20library%20%28works%20on%20%23Java8%20%23Java9%20%23Java10%20%23Java11%20%23Java12%20%23Java13%20%23Java14%20%23Java15-ea%29&url=https://github.com/burningwave/core%23burningwave-core-)

<a href="https://www.burningwave.org">
<img src="https://raw.githubusercontent.com/burningwave/core/master/Burningwave-logo.png" alt="Burningwave-logo.png" height="180px" align="right"/>
</a>

[![Maven Central with version prefix filter](https://img.shields.io/maven-central/v/org.burningwave/core/7)](https://maven-badges.herokuapp.com/maven-central/org.burningwave/core/)
[![GitHub](https://img.shields.io/github/license/burningwave/core)](https://github.com/burningwave/core/blob/master/LICENSE)

[![Platforms](https://img.shields.io/badge/platforms-Windows%2C%20Max%20OS%2C%20Linux-orange)](https://github.com/burningwave/core/actions/runs/167454297)

[![Supported JVM](https://img.shields.io/badge/supported%20JVM-8%2C%209%2C%2010%2C%2011%2C%2012%2C%2013%2C%2014%2C%2015ea-blueviolet)](https://github.com/burningwave/core/actions/runs/167454297)

[![Coveralls github branch](https://img.shields.io/coveralls/github/burningwave/core/master)](https://coveralls.io/github/burningwave/core?branch=master)
[![GitHub issues](https://img.shields.io/github/issues/burningwave/core)](https://github.com/burningwave/core/issues)

**Tested on Java versions ranging from 8 to 15-ea, Burningwave Core** is a fully independent, advanced, free and open source Java frameworks building library and it is useful for scanning class paths, generating classes at runtime, facilitating the use of reflection, scanning the filesystem, executing stringified source code and much more...

Burningwave Core contains **THE MOST POWERFUL CLASSPATH SCANNER** for criteria based classes search: it’s possible to search classes by every criteria that your immagination can made by using lambda expressions; **scan engine is highly optimized using direct allocated ByteBuffers to avoid heap saturation; searches are executed in multithreading context and are not affected by “_the issue of the same class loaded by different classloaders_”** (normally if you try to execute "isAssignableFrom" method on a same class loaded from different classloader it returns false).

And now we will see:
* [**including Burningwave Core in your project**](#Including-Burningwave-Core-in-your-project)
* [**generating classes at runtime and invoking their methods with and without the use of reflection**](#Generating-classes-at-runtime-and-invoking-their-methods-with-and-without-the-use-of-reflection)
* [**executing stringified source code**](#Executing-stringified-source-code)
* [**retrieving classes of runtime class paths or of other paths through the ClassHunter**](#Retrieving-classes-of-runtime-class-paths-or-of-other-paths-through-the-ClassHunter)
* [**finding where a class is loaded from**](#Finding-where-a-class-is-loaded-from)
* [**reaching a resource of the file system**](#Reaching-a-resource-of-the-file-system)
* [**resolving, collecting or retrieving paths**](#Resolving-collecting-or-retrieving-paths)
* [**retrieving placeholdered items from map and properties file**](#Retrieving-placeholdered-items-from-map-and-properties-file)
* [**handling privates and all other members of an object**](#Handling-privates-and-all-other-members-of-an-object)
* [**getting and setting properties of a Java bean through path**](#Getting-and-setting-properties-of-a-Java-bean-through-path)
* [**architectural overview and configuration**](#Architectural-overview-and-configuration)
* [**other examples of using some components**](#Other-examples-of-using-some-components)

<br/>

**For assistance you can [subscribe](https://www.burningwave.org/registration/) to the [forum](https://www.burningwave.org/forum/) and then ask in the topic ["How to do?"](https://www.burningwave.org/forum/forum/how-to/) or you can ask on [Stack Overflow](https://stackoverflow.com/questions/tagged/burningwave)**.

<br/>

# Including Burningwave Core in your project 
To include Burningwave Core library in your projects simply use with **Apache Maven**:

```xml
<dependency>
    <groupId>org.burningwave</groupId>
    <artifactId>core</artifactId>
    <version>7.20.0</version>
</dependency>
```

<br/>

# Generating classes at runtime and invoking their methods with and without the use of reflection

For this purpose is necessary the use of **ClassFactory** component and of the **sources generating components**. Once the sources have been set in **UnitSourceGenerator** objects, they must be passed to **loadOrBuildAndDefine** method of ClassFactory with the ClassLoader where you want to define new generated classes. This method performs the following operations: tries to load all the classes present in the UnitSourceGenerator through the class loader, if at least one of these is not found it proceeds to compiling all the UnitSourceGenerators and uploading their classes on class loader: in this case, keep in mind that if a class with the same name was previously loaded by the class loader, the compiled class will not be uploaded. Once the classes have been compiled and loaded, it is possible to invoke their methods in severals ways as shown at the end of the example below. **For more examples you can go [here](https://github.com/burningwave/core/tree/master/src/test/java/org/burningwave/core/examples/classfactory) where you can also find an [example about the generation of classes by using libraries located outside the runtime class paths](https://github.com/burningwave/core/blob/master/src/test/java/org/burningwave/core/examples/classfactory/ExternalClassRuntimeExtender.java)**.
```java
package org.burningwave.core.examples.classfactory;

import static org.burningwave.core.assembler.StaticComponentContainer.Constructors;

import java.lang.reflect.Modifier;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.burningwave.core.Virtual;
import org.burningwave.core.assembler.ComponentContainer;
import org.burningwave.core.assembler.ComponentSupplier;
import org.burningwave.core.classes.AnnotationSourceGenerator;
import org.burningwave.core.classes.ClassFactory;
import org.burningwave.core.classes.ClassSourceGenerator;
import org.burningwave.core.classes.FunctionSourceGenerator;
import org.burningwave.core.classes.GenericSourceGenerator;
import org.burningwave.core.classes.TypeDeclarationSourceGenerator;
import org.burningwave.core.classes.UnitSourceGenerator;
import org.burningwave.core.classes.VariableSourceGenerator;

public class RuntimeClassExtender {

    @SuppressWarnings("resource")
    public static void execute() throws Throwable {
        UnitSourceGenerator unitSG = UnitSourceGenerator.create("packagename").addClass(
            ClassSourceGenerator.create(
                TypeDeclarationSourceGenerator.create("MyExtendedClass")
            ).addModifier(
                Modifier.PUBLIC
            //generating new method that override MyInterface.convert(LocalDateTime)
            ).addMethod(
                FunctionSourceGenerator.create("convert")
                .setReturnType(
                    TypeDeclarationSourceGenerator.create(Comparable.class)
                    .addGeneric(GenericSourceGenerator.create(Date.class))
                ).addParameter(VariableSourceGenerator.create(LocalDateTime.class, "localDateTime"))
                .addModifier(Modifier.PUBLIC)
                .addAnnotation(AnnotationSourceGenerator.create(Override.class))
                .addBodyCodeRow("return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());")
                .useType(ZoneId.class)
            ).addConcretizedType(
                MyInterface.class
            ).expands(ToBeExtended.class)
        );
        System.out.println("\nGenerated code:\n" + unitSG.make());
        //With this we store the generated source to a path
        unitSG.storeToClassPath(System.getProperty("user.home") + "/Desktop/bw-tests");
        ComponentSupplier componentSupplier = ComponentContainer.getInstance();
        ClassFactory classFactory = componentSupplier.getClassFactory();
        //this method compile all compilation units and upload the generated classes to default
        //class loader declared with property "class-factory.default-class-loader" in 
        //burningwave.properties file (see "Overview and configuration").
        //If you need to upload the class to another class loader use
        //loadOrBuildAndDefine(LoadOrBuildAndDefineConfig) method
        Class<?> generatedClass = classFactory.loadOrBuildAndDefine(
            unitSG
        ).get(
            "packagename.MyExtendedClass"
        );
        ToBeExtended generatedClassObject =
            Constructors.newInstanceOf(generatedClass);
        generatedClassObject.printSomeThing();
        System.out.println(
            ((MyInterface)generatedClassObject).convert(LocalDateTime.now()).toString()
        );
        //You can also invoke methods by casting to Virtual (an interface offered by the
        //library for faciliate use of runtime generated classes)
        Virtual virtualObject = (Virtual)generatedClassObject;
        //Invoke by using reflection
        virtualObject.invoke("printSomeThing");
        //Invoke by using MethodHandle
        virtualObject.invokeDirect("printSomeThing");
        System.out.println(
            ((Date)virtualObject.invokeDirect("convert", LocalDateTime.now())).toString()
        );
    }   

    public static class ToBeExtended {

        public void printSomeThing() {
            System.out.println("Called method printSomeThing");
        }

    }

    public static interface MyInterface {

        public Comparable<Date> convert(LocalDateTime localDateTime);

    }

    public static void main(String[] args) throws Throwable {
        execute();
    }
}
```

<br/>

# Executing stringified source code
It is possible to execute stringified source code by using the **CodeExecutor** in three three different ways:
* [through **BodySourceGenerator**](#Executing-code-with-BodySourceGenerator)
* [through a property located in Burningwave configuration file](#Executing-code-of-a-property-located-in-Burningwave-configuration-file)
* [through a property located in a custom Properties file](#Executing-code-of-a-property-located-in-a-custom-properties-file)

<br/>

## Executing code with BodySourceGenerator
For first way we must create a **ExecuteConfig** by using the within static method **forBodySourceGenerator** to which must be passed the **BodySourceGenerator** that contains the source code with the parameters used within: after that we must pass the created configuration to the **execute** method of CodeExecutor as shown below:
```java
package org.burningwave.core.examples.codeexecutor;

import java.util.ArrayList;
import java.util.List;

import org.burningwave.core.assembler.ComponentContainer;
import org.burningwave.core.assembler.ComponentSupplier;
import org.burningwave.core.classes.ExecuteConfig;
import org.burningwave.core.classes.BodySourceGenerator;

public class SourceCodeExecutor {
    
    public static Integer execute() {
        ComponentSupplier componentSupplier = ComponentContainer.getInstance();
        return componentSupplier.getCodeExecutor().execute(
            ExecuteConfig.forBodySourceGenerator(
                BodySourceGenerator.createSimple().useType(ArrayList.class, List.class)
                .addCodeRow("System.out.println(\"number to add: \" + parameter[0]);")
                .addCodeRow("List<Integer> numbers = new ArrayList<>();")
                .addCodeRow("numbers.add((Integer)parameter[0]);")
                .addCodeRow("System.out.println(\"number list size: \" + numbers.size());")
                .addCodeRow("System.out.println(\"number in the list: \" + numbers.get(0));")
                .addCodeRow("Integer inputNumber = (Integer)parameter[0];")
                .addCodeRow("return (T)new Integer(inputNumber + (Integer)parameter[1]);")
            ).withParameter(Integer.valueOf(5), Integer.valueOf(3))
        );
        
    }
    
    public static void main(String[] args) {
        System.out.println("Total is: " + execute());
    }
}
```

<br/>

## Executing code of a property located in Burningwave configuration file
To execute code from Burningwave configuration file ([**burningwave.properties**](#configuration-1) or other file that we have used to create the ComponentContainer: [**see architectural overview and configuration**](#Architectural-overview-and-configuration)) we must add to it a  property that contains the code and, if it is necessary to import classes, we must add them to another property named as the property that contains the code plus the suffix **'imports'**. E.g:
```properties
code-block-1=\
    Date now= new Date();\
    return (T)now;
code-block-1.imports=java.util.Date;
```
It is also possible to include the code of a property in another property:
```properties
code-block-1=\
    ${code-block-2}\
    return (T)Date.from(zonedDateTime.toInstant());
code-block-1.imports=\
    ${code-block-2.imports}\
    java.util.Date;
code-block-2=\
    LocalDateTime localDateTime = (LocalDateTime)parameter[0];\
    ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.systemDefault());
code-block-2.imports=\
    static org.burningwave.core.assembler.StaticComponentContainer.Strings;\
    java.time.LocalDateTime;\
    java.time.ZonedDateTime;\
    java.time.ZoneId;
```
After that, for executing the code of the property we must call the **executeProperty** method of CodeExecutor and passing to it the property name to be executed and the parameters used in the property code:
```java
package org.burningwave.core.examples.codeexecutor;

import java.time.LocalDateTime;

import org.burningwave.core.assembler.ComponentContainer;
import org.burningwave.core.assembler.ComponentSupplier;

public class SourceCodeExecutor {
    
    public static void execute() {
        ComponentSupplier componentSupplier = ComponentContainer.getInstance();
        System.out.println("Time is: " +
            componentSupplier.getCodeExecutor().executeProperty("code-block-1", LocalDateTime.now())    
        );
    }
    
    public static void main(String[] args) {
        execute();
    }
}
```

<br/>

## Executing code of a property located in a custom properties file
To execute code from a custom properties file we must add to it a  property that contains the code and, if it is necessary to import classes, we must add them to another property named as the property that contains the code plus the suffix **'imports'**. E.g:
```properties
code-block-1=\
    Date now= new Date();\
    return (T)now;
code-block-1.imports=java.util.Date;
```
It is also possible to include the code of a property in another property:
```properties
code-block-1=\
    ${code-block-2}\
    return (T)Date.from(zonedDateTime.toInstant());
code-block-1.imports=\
    ${code-block-2.imports}\
    java.util.Date;
code-block-2=\
    LocalDateTime localDateTime = (LocalDateTime)parameter[0];\
    ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.systemDefault());
code-block-2.imports=\
    static org.burningwave.core.assembler.StaticComponentContainer.Strings;\
    java.time.LocalDateTime;\
    java.time.ZonedDateTime;\
    java.time.ZoneId;
```
After that, for executing the code of the property we must create an **ExecuteConfig** object and set on it:
* the path (relative or absolute) of our custom properties file 
* the property name to be executed 
* the parameters used in the property code

Then we must call the **execute** method of CodeExecutor with the created ExecuteConfig object:
```java
package org.burningwave.core.examples.codeexecutor;

import java.time.LocalDateTime;

import org.burningwave.core.assembler.ComponentContainer;
import org.burningwave.core.assembler.ComponentSupplier;
import org.burningwave.core.classes.ExecuteConfig;

public class SourceCodeExecutor {
    
    public static void execute() {
        ComponentSupplier componentSupplier = ComponentContainer.getInstance();
        System.out.println("Time is: " +
            componentSupplier.getCodeExecutor().execute(
                ExecuteConfig.forPropertiesFile("custom-folder/code.properties")
                //Uncomment the line below if the path you have supplied is an absolute path
                //.setFilePathAsAbsolute(true)
                .setPropertyName("code-block-1")
                .withParameter(LocalDateTime.now())
            )    
        );
    }
    
    public static void main(String[] args) {
        execute();
    }
}
```

<br/>

# Retrieving classes of runtime class paths or of other paths through the ClassHunter
The compononents of the class paths scanning engine are: **ByteCodeHunter**, **ClassHunter** and the **ClassPathHunter**. Now we are going to use the ClassHunter to search for all classes that have package name that matches a regex. So in this example we're looking for all classes whose package name contains "springframework" string in the runtime class paths
```java
import java.util.Collection;

import org.burningwave.core.assembler.ComponentContainer;
import org.burningwave.core.assembler.ComponentSupplier;
import org.burningwave.core.classes.ClassCriteria;
import org.burningwave.core.classes.CacheableSearchConfig;
import org.burningwave.core.classes.ClassHunter;
import org.burningwave.core.classes.ClassHunter.SearchResult;
import org.burningwave.core.classes.SearchConfig;
import org.burningwave.core.io.PathHelper;
    
public class Finder {
    
    public Collection<Class<?>> find() {
        ComponentSupplier componentSupplier = ComponentContainer.getInstance();
        PathHelper pathHelper = componentSupplier.getPathHelper();
        ClassHunter classHunter = componentSupplier.getClassHunter();
        
        CacheableSearchConfig searchConfig = SearchConfig.forPaths(
            //Here you can add all absolute path you want:
            //both folders, zip, jar, ear and war will be recursively scanned.
            //For example you can add: "C:\\Users\\user\\.m2", or a path of
            //an ear file that contains nested war with nested jar files
            //With the row below the search will be executed on runtime Classpaths
            pathHelper.getMainClassPaths()
            //If you want to scan only one jar or some certain jars you can use, for example,
            //this commented line of code instead "pathHelper.getMainClassPaths()":
            //pathHelper.getPaths(path -> path.contains("spring-core-4.3.4.RELEASE.jar"))
        ).by(
            ClassCriteria.create().allThat((cls) -> {
                return cls.getPackage().getName().matches(".*springframework.*");
            })
        );
        //The loadInCache method loads all classes in the paths of the SearchConfig received as input
        //and then execute the queries of the ClassCriteria on the cached data. Once the data has been 
        //cached, it is possible to take advantage of faster searches for the loaded paths also through 
        //the findBy method. In addition to the loadCache method, loading data into the cache can also
        //take place via the findBy method if the latter receives a SearchConfig without ClassCriteria
        //as input. It is possible to clear the cache individually for every hunter (ClassHunter, 
        //ByteCodeHunter and ClassPathHunter) with clearCache method but to avoid inconsistencies 
        //it is recommended to perform this cleaning using the clearHuntersCache method of the ComponentSupplier.
        //To perform searches that do not use the cache you must intantiate the search configuration with 
        //SearchConfig.withoutUsingCache() method
        SearchResult searchResult = classHunter.loadInCache(searchConfig).find();
        
        return searchResult.getClasses();
    }
    
}
```

<br/>

# Finding where a class is loaded from

For this purpose we are going to use the **ClassPathHunter** component:
```java
import java.util.Collection;

import org.burningwave.core.assembler.ComponentContainer;
import org.burningwave.core.assembler.ComponentSupplier;
import org.burningwave.core.classes.ClassCriteria;
import org.burningwave.core.classes.CacheableSearchConfig;
import org.burningwave.core.classes.ClassPathHunter;
import org.burningwave.core.classes.ClassPathHunter.SearchResult;
import org.burningwave.core.classes.SearchConfig;
import org.burningwave.core.io.FileSystemItem;
import org.burningwave.core.io.PathHelper;

public class Finder {

    public Collection<FileSystemItem> find() {
        ComponentSupplier componentSupplier = ComponentContainer.getInstance();
        PathHelper pathHelper = componentSupplier.getPathHelper();
        ClassPathHunter classPathHunter = componentSupplier.getClassPathHunter();

        CacheableSearchConfig searchConfig = SearchConfig.forPaths(
            //Here you can add all absolute path you want:
            //both folders, zip and jar will be recursively scanned.
            //For example you can add: "C:\\Users\\user\\.m2"
            //With the row below the search will be executed on runtime Classpaths
            pathHelper.getMainClassPaths()
        ).by(
            ClassCriteria.create().allThat(cls ->
                cls.getName().equals("Finder")      
            )
        );        

        SearchResult searchResult = classPathHunter.loadInCache(searchConfig).find();
        return searchResult.getClassPaths();
    }

}
```

<br/>

# Reaching a resource of the file system
Through **FileSystemItem** you can reach a resource of the file system even if it is contained in a nested supported (**zip, jar, war, ear, jmod**) compressed archive and obtain the content of it or other informations such as if it is a folder or a file or a compressed archive or if it is a compressed entry or obtain, if it is a folder or a compressed archive, the direct children or all nested children or a filtered collection of them. You can retrieve a FileSystemItem through an absolute path or through a relative path referred to your classpath by using the PathHelper. FileSystemItems are cached and **there will only be one instance of them for an absolute path** and you can also clear the cache e reload all informations of a FileSystemItem. In the example below we show how to retrieve and use a FileSystemItem.

```java
package org.burningwave.core.examples.filesystemitem;

import org.burningwave.core.assembler.ComponentContainer;
import org.burningwave.core.io.FileSystemItem;

public class ResourceReacher {
    
    private static void execute() {
        //Obtaining FileSystemItem through absolute path
        FileSystemItem fSI = FileSystemItem.ofPath("C:/Program Files (x86)");
       
        
        FileSystemItem firstFolderFound = null;
        
        //Obtaining direct children
        for (FileSystemItem child : fSI.getChildren()) {
            System.out.println("child name:" + child.getAbsolutePath());
            if (firstFolderFound == null && child.isFolder()) {
                 System.out.println(child.getAbsolutePath() + " is a folder: " + child.isFolder());
                 firstFolderFound = child;
            }
        }
        
        //Filtering all nested children for extension
        for (FileSystemItem child : firstFolderFound.findInAllChildren(
            FileSystemItem.Criteria.forAllFileThat(fSIC -> 
                "txt".equals(fSIC.getExtension()) || "exe".equals(fSIC.getExtension()))
            )
        ){
            System.out.println("child name: " + child.getName() + " - child parent: " + child.getParent().getName());
            //copy the file to a folder
            child.copyTo(System.getProperty("user.home") + "/Desktop/copy");
        }
        
        //Obtaining a FileSystemItem through a relative path (in this case we are obtaining a reference to a jar
        //contained in an ear that is contained in a zip
        fSI = ComponentContainer.getInstance().getPathHelper().getResource(
            "/../../src/test/external-resources/libs-for-test.zip/ESC-Lib.ear/APP-INF/lib/jaxb-xjc-2.1.7.jar"
        );
        
        System.out.println("is an archive:" + fSI.isArchive());
        
        //This method return true if the file or folder is located inside a compressed archive
        System.out.println("is compressed:" + fSI.isCompressed());
        
        //this clear cache
        fSI.refresh(true);
        
        //Obtaining direct children
        for (FileSystemItem child : fSI.getChildren()) {
            System.out.println("child name:" + child.getAbsolutePath());
        }
        
        //Obtaining all nested children
        for (FileSystemItem child : fSI.getAllChildren()) {
            System.out.println("child name:" + child.getAbsolutePath());
        }
        
        //Obtaining the content of the resource (once the content is loaded it will be cached)
        fSI.toByteBuffer();
    }
    
    public static void main(String[] args) {
        execute();
    }
    
}
```

<br/>

# Resolving, collecting or retrieving paths

Through **PathHelper** we can resolve or collect paths or retrieving resources even through supported archive files (zip, jar, jmod, ear and war).
So we can create a path collection by adding an entry in **[burningwave.properties](#configuration-1)** file that **starts with 'paths.' prefix (this is a fundamental requirement to allow PathHelper to load the paths)**, e.g.:
```properties
paths.my-collection=c:/some folder;C:/some folder 2/ some folder 3;
paths.my-collection-2=c:/some folder 4;C:/some folder 6;
```
These paths could be retrieved through **PathHelper.getPaths** method and we can find a resource in all configured paths plus the runtime class paths (that is automatically loaded under the entry named **'paths.main-class-paths'**) by using **PathHelper.getResource** method, e.g.:
```java
ComponentSupplier componentSupplier = ComponentContainer.getInstance();
PathHelper pathHelper = componentSupplier.getPathHelper();
Collection<String> paths = pathHelper.getPaths("paths.my-collection", "paths.my-collection-2"));
//With the code below all configured paths plus runtime class paths will be iterated to search
//the resource called some.jar
FileSystemItem resource = pathHelper.getResource("/../some.jar");
InputStream inputStream = resource.toInputStream();
```
We can also use placeholder and relative paths, e.g.:
```properties
paths.my-collection-3=C:/some folder 2/ some folder 3;
paths.my-jar=${paths.my-collection-3}/../some.jar;
```
It is also possibile to obtain references to resources of the runtime class paths by using the pre-loaded entry 'paths.main-class-paths' (runtime class paths are automatically iterated for searching the path that match the entry), e.g.:
```properties
paths.my-jar=${paths.main-class-paths}/../some.jar;
```
We can also use a [**FileSystemItem**](#Reaching-a-resource-of-the-file-system) listing (**FSIL**) expression and, for example, create a path collection of all absolute path of all classes of the runtime class paths:
```properties
paths.all-runtime-classes=//${paths.main-class-paths}//allChildren:.*?\.classes;
```
A **FSIL** expression encloses in a couple of double slash an absolute path or a placeholdered path collection that will be scanned; after the second double slash we have the listing type that could refear to direct children of scanned paths ('**children**') or to all nested children of scanned paths ('**allChildren**'); after that and colons we have the regular expression with we are going to filter the absolute paths iterated.

<br/>

# Retrieving placeholdered items from map and properties file

With **IterableObjectHelper** component it is possible to retrieve items from map by using placeholder or not. In the following example we are going to show how to retrieve strings or objects from **[burningwave.properties](#configuration-1)** file and from maps.

**[burningwave.properties](#configuration-1)** file:
```properties
...
code-block-1=\
    ${code-block-2}\
    return (T)Date.from(zonedDateTime.toInstant());
code-block-2=\
    LocalDateTime localDateTime = (LocalDateTime)parameter[0];\
    ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.systemDefault());
...
```
**Java code**:
```java
package org.burningwave.core.examples.iterableobjecthelper;

import static org.burningwave.core.assembler.StaticComponentContainer.IterableObjectHelper;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.burningwave.core.assembler.ComponentContainer;
import org.burningwave.core.assembler.ComponentSupplier;
import org.burningwave.core.io.PathHelper;

public class ItemFromMapRetriever {
    
    public void execute() throws IOException {
        ComponentSupplier componentSupplier = ComponentContainer.getInstance();
        PathHelper pathHelper = componentSupplier.getPathHelper();
        Properties properties = new Properties();
        properties.load(pathHelper.getResourceAsStream("burningwave.properties"));
        String code = IterableObjectHelper.resolveStringValue(properties, "code-block-1");        
        
        Map<Object, Object> map = new HashMap<>();
        map.put("class-loader-01", "${class-loader-02}");
        map.put("class-loader-02", "${class-loader-03}");
        map.put("class-loader-03", Thread.currentThread().getContextClassLoader().getParent());
        ClassLoader parentClassLoader = IterableObjectHelper.resolveValue(map, "class-loader-01");
        
        map.clear();
        map.put("class-loaders", "${class-loader-02};${class-loader-03};");
        map.put("class-loader-02", Thread.currentThread().getContextClassLoader());
        map.put("class-loader-03", Thread.currentThread().getContextClassLoader().getParent());
        Collection<ClassLoader> classLoaders = IterableObjectHelper.resolveValues(map, "class-loaders", ";");
    }
    
    public static void main(String[] args) throws IOException {
        new ItemFromMapRetriever().execute();
    }
}
```
<br>

# Handling privates and all other members of an object
Through **Fields**, **Constructors** and **Methods** components it is possible to get or set fields value, invoking or finding constructors or methods of an object.
Members handlers use to cache all members for faster access.
For fields handling we are going to use **Fields** component:
```java
import static org.burningwave.core.assembler.StaticComponentContainer.Fields;

import java.util.Collection;
import java.util.Map;


public class FieldsHandler {
    
    public static void execute() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        //Fast access by memory address
        Collection<Class<?>> loadedClasses = Fields.getDirect(classLoader, "classes");
        //Access by Reflection
        loadedClasses = Fields.get(classLoader, "classes");
        
        //Getting all field values of an object through memory address access
        Map<String, Object> values = Fields.getAllDirect(classLoader);
        //Getting all field values of an object through reflection access
        values = Fields.getAll(classLoader);
        Object obj = new Object() {
            List<Object> objectValue;
        };
        List<Object> objectValue = new ArrayList<>();
        //Setting field value through memory address access
        Fields.setDirect(obj, "objectValue", objectValue);
        List<Object> objectValue2Var = Fields.getDirect(obj, "objectValue");
    }
    
    public static void main(String[] args) {
        execute();
    } 
}
```
For methods handling we are going to use **Methods** component:
```java
import static org.burningwave.core.assembler.StaticComponentContainer.Methods;

public class MethodsHandler {
    
    public static void execute() {
        //Invoking method by using reflection
        Methods.invoke(System.out, "println", "Hello World");
        
        //Invoking method by using MethodHandle
        Integer number = Methods.invokeDirect(Integer.class, "valueOf", 1);
    }
    
    public static void main(String[] args) {
        execute();
    }
}
```

For constructors handling we are going to use **Constructors** component:
```java
import static org.burningwave.core.assembler.StaticComponentContainer.Constructors;

import org.burningwave.core.classes.MemoryClassLoader;

public class ConstructorsHandler {
    
    public static void execute() {
        //Invoking constructor by using reflection
        MemoryClassLoader classLoader = Constructors.newInstanceOf(MemoryClassLoader.class, Thread.currentThread().getContextClassLoader());
        
        //Invoking constructor with a null parameter value by using MethodHandle
        classLoader = Constructors.newInstanceDirectOf(MemoryClassLoader.class, null);
    }
    
    public static void main(String[] args) {
        execute();
    }
    
}
```

<br>

# Getting and setting properties of a Java bean through path
Through **ByFieldOrByMethodPropertyAccessor** and **ByMethodOrByFieldPropertyAccessor** it is possible to get and set properties of a Java bean by using path. So for this example we will use these Java beans:

```java
package org.burningwave.core.bean;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Complex {
    private Complex.Data data;
    
    public Complex() {
        setData(new Data());
    }
    
    
    public Complex.Data getData() {
        return data;
    }
    
    public void setData(Complex.Data data) {
        this.data = data;
    }


    public static class Data {
        private Data.Item[][] items;
        private List<Data.Item> itemsList;
        private Map<String, Data.Item[][]> itemsMap;
        
        public Data() {
            items = new Data.Item[][] {
                new Data.Item[] {
                    new Item("Hello"),
                    new Item("World!"),
                    new Item("How do you do?")
                },
                new Data.Item[] {
                    new Item("How do you do?"),
                    new Item("Hello"),
                    new Item("Bye")
                }
            };
            itemsMap = new LinkedHashMap<>();
            itemsMap.put("items", items);
        }
        
        public Data.Item[][] getItems() {
            return items;
        }
        public void setItems(Data.Item[][] items) {
            this.items = items;
        }
        
        public List<Data.Item> getItemsList() {
            return itemsList;
        }
        public void setItemsList(List<Data.Item> itemsList) {
            this.itemsList = itemsList;
        }
        
        public Map<String, Data.Item[][]> getItemsMap() {
            return itemsMap;
        }
        public void setItemsMap(Map<String, Data.Item[][]> itemsMap) {
            this.itemsMap = itemsMap;
        }
        
        public static class Item {
            private String name;
            
            public Item(String name) {
                this.name = name;
            }
            
            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }
        }
    }
}
```
... And now we are going to get and set some properties:
```java
import static org.burningwave.core.assembler.StaticComponentContainer.ByFieldOrByMethodPropertyAccessor;
import static org.burningwave.core.assembler.StaticComponentContainer.ByMethodOrByFieldPropertyAccessor;

import org.burningwave.core.bean.Complex;

public class GetAndSetPropertiesThroughPath{
    
    public void execute() {
        Complex complex = new Complex();
        //This type of property accessor try to access by field introspection: if no field was found
        //it will search getter method and invokes it
        String nameFromObjectInArray = ByFieldOrByMethodPropertyAccessor.get(complex, "data.items[1][0].name");
        String nameFromObjectMap = ByFieldOrByMethodPropertyAccessor.get(complex, "data.itemsMap[items][1][1].name");
        System.out.println(nameFromObjectInArray);
        System.out.println(nameFromObjectMap);
        //This type of property accessor looks for getter method and invokes it: if no getter method was found
        //it will search for field and try to retrieve it
        nameFromObjectInArray = ByMethodOrByFieldPropertyAccessor.get(complex, "data.items[1][2].name");
        nameFromObjectMap = ByMethodOrByFieldPropertyAccessor.get(complex, "data.itemsMap[items][1][1].name");
        System.out.println(nameFromObjectInArray);
        System.out.println(nameFromObjectMap);
        ByMethodOrByFieldPropertyAccessor.set(complex, "data.itemsMap[items][1][1].name", "Good evening!");
        nameFromObjectInArray = ByMethodOrByFieldPropertyAccessor.get(complex, "data.itemsMap[items][1][1].name");
        System.out.println(nameFromObjectInArray);
    }
    
    public static void main(String[] args) {
        new GetAndSetPropertiesThroughPath().execute();
    }
    
}
```


<br/>

# Architectural overview and configuration

**Burningwave Core** is based on the concept of component and component container. A **component** is a dynamic object that perform functionality related to the domain it belong to.
A **component container** contains a set of dynamic components and could be of two types:
* **static component container**
* **dynamic component container**

More than one dynamic container can be created, while only one static container can exists.
<br/>

## Static component container
It is represented by the **org.burningwave.core.assembler.StaticComponentContainer** class that provides the following fields for each component supplied:
```java
public static final org.burningwave.core.classes.PropertyAccessor ByFieldOrByMethodPropertyAccessor;
public static final org.burningwave.core.classes.PropertyAccessor ByMethodOrByFieldPropertyAccessor;
public static final org.burningwave.core.jvm.LowLevelObjectsHandler.ByteBufferDelegate ByteBufferDelegate;
public static final org.burningwave.core.Cache Cache;
public static final org.burningwave.core.classes.Classes Classes;
public static final org.burningwave.core.classes.Classes.Loaders ClassLoaders;
public static final org.burningwave.core.classes.Constructors Constructors;
public static final org.burningwave.core.io.FileSystemHelper FileSystemHelper;
public static final org.burningwave.core.classes.Fields Fields;
public static final org.burningwave.core.iterable.Properties GlobalProperties;
public static final org.burningwave.core.iterable.IterableObjectHelper IterableObjectHelper;
public static final org.burningwave.core.jvm.JVMInfo JVMInfo;
public static final org.burningwave.core.jvm.LowLevelObjectsHandler LowLevelObjectsHandler;
public static final org.burningwave.core.ManagedLogger.Repository ManagedLoggersRepository;
public static final org.burningwave.core.classes.Members Members;
public static final org.burningwave.core.classes.Methods Methods;
public static final org.burningwave.core.Strings.Paths Paths;
public static final org.burningwave.core.io.Resources Resources;
public static final org.burningwave.core.io.Streams Streams;
public static final org.burningwave.core.classes.SourceCodeHandler SourceCodeHandler;
public static final org.burningwave.core.Strings Strings;
public static final org.burningwave.core.Throwables Throwables;
```

... That can be used within your application, simply adding a static import to your compilation unit, i.e.:
```java
package org.burningwave.core.examples.staticcomponents;

import static org.burningwave.core.assembler.StaticComponentContainer.Classes;
import static org.burningwave.core.assembler.StaticComponentContainer.ManagedLoggersRepository;

public class UseOfStaticComponentsExample {
    
    public void yourMethod(){
        ManagedLoggersRepository.logInfo(UseOfStaticComponentsExample.class, Classes.getId(this));
    }

}
```
### Configuration
The configuration of this type of container is done via **burningwave.static.properties** file or via **burningwave.static.default.properties** file: the library searches for the first file and if it does not find it, then it searches for the second file and if neither this one is found then the library sets the default configuration programmatically. **The default configuration loaded programmatically if no configuration file is found is the following**:
```properties
#With this value the library will search if org.slf4j.Logger is present and, in this case,
#the SLF4JManagedLoggerRepository will be instantiated, otherwise the SimpleManagedLoggerRepository will be instantiated
managed-logger.repository=autodetect
#to increase performance set it to false
managed-logger.repository.enabled=true
static-component-container.clear-temporary-folder-on-init=true
static-component-container.hide-banner-on-init=false
streams.default-buffer-size=1024
streams.default-byte-buffer-allocation-mode=ByteBuffer::allocateDirect
```
**If in your custom burningwave.static.properties or burningwave.static.default.properties file one of this default properties is not found, the relative default value here in the box above is assumed**.
Here an example of a **burningwave.static.properties** file with all configurable properties:
```properties
#other possible values are: autodetect, org.burningwave.core.SimpleManagedLoggerRepository
managed-logger.repository=org.burningwave.core.SLF4JManagedLoggerRepository
#to increase performance set it to false
managed-logger.repository.enabled=true
managed-logger.repository.logging.debug.disabled-for=\
    org.burningwave.core.io.FileSystemItem;\
    org.burningwave.core.classes.PathMemoryClassLoader;\
    org.burningwave.core.classes.MemoryClassLoader;
streams.default-buffer-size=0.5Kb
#other possible value is ByteBuffer::allocate
streams.default-byte-buffer-allocation-mode=ByteBuffer::allocateDirect
static-component-container.clear-temporary-folder-on-init=true
static-component-container.hide-banner-on-init=false
```
<br/>

## Dynamic component container
It is represented by the **org.burningwave.core.assembler.ComponentContainer** class that provides the following methods for each component supplied:
```java
public ByteCodeHunter getByteCodeHunter();
public ClassFactory getClassFactory();
public ClassHunter getClassHunter();
public ClassPathHunter getClassPathHunter();
public CodeExecutor getCodeExecutor();
public FunctionalInterfaceFactory getFunctionalInterfaceFactory();
public JavaMemoryCompiler getJavaMemoryCompiler();
public PathHelper getPathHelper();
public PathScannerClassLoader getPathScannerClassLoader();
```
... That can be used within your application, simply as follow:
```java
package org.burningwave.core.examples.componentcontainer;

import org.burningwave.core.assembler.ComponentContainer;
import org.burningwave.core.assembler.ComponentSupplier;
import org.burningwave.core.classes.ClassFactory;
import org.burningwave.core.classes.ClassHunter;
import org.burningwave.core.io.PathHelper;
import org.burningwave.core.iterable.Properties;

public class RetrievingDynamicComponentContainerAndComponents {

    public static void execute() throws Throwable {
        //In this case we are retrieving the singleton component container instance
        ComponentSupplier componentSupplier = ComponentContainer.getInstance();
        
        //In this case we are creating a component container by using a custom configuration file
        ComponentSupplier customComponentSupplier = ComponentContainer.create("your-custom-properties-file.properties");
        
        //In this case we are creating a component container programmatically by using a custom properties object
        Properties configProps = new Properties();
        configProps.put(ClassFactory.Configuration.Key.DEFAULT_CLASS_LOADER, Thread.currentThread().getContextClassLoader());
        configProps.put(ClassHunter.Configuration.Key.DEFAULT_PATH_SCANNER_CLASS_LOADER, componentSupplier.getPathScannerClassLoader());
        ComponentSupplier customComponentSupplier2 = ComponentContainer.create(configProps);
        
        PathHelper pathHelper = componentSupplier.getPathHelper();
        ClassFactory classFactory = customComponentSupplier.getClassFactory();
        ClassHunter classHunter = customComponentSupplier2.getClassHunter();
       
    }   
    
}
```
### Configuration
The configuration of this type of container can be done via Properties file or programmatically via a Properties object.
If you use the singleton instance obtained via ComponentContainer.getInstance() method, you must create a **burningwave.properties** file and put it on base path of your classpath project.
**The default configuration automatically loaded if no configuration file is found is the following**:
```properties
class-factory.byte-code-hunter.search-config.check-file-option=\
    ${hunters.default-search-config.check-file-option}
#default classloader used by the ClassFactory to load generated classes
class-factory.default-class-loader=\
    (Supplier<ClassLoader>)() -> ((ComponentSupplier)parameter[0]).getPathScannerClassLoader()
#This variable is empty by default and can be valorized by developer and it is
#included by 'class-factory.default-class-loader.imports' property
class-factory.default-class-loader.additional-imports=
class-factory.default-class-loader.imports=\  
    ${class-factory.default-class-loader.additional-imports};\
    org.burningwave.core.assembler.ComponentSupplier;\
    java.util.function.Function;\
    org.burningwave.core.io.FileSystemItem;\
    org.burningwave.core.classes.PathScannerClassLoader;\
    java.util.function.Supplier;
class-factory.default-class-loader.name=\
    org.burningwave.core.classes.DefaultClassLoaderRetrieverForClassFactory
class-hunter.default-path-scanner-class-loader=\
    (Supplier<PathScannerClassLoader>)() -> ((ComponentSupplier)parameter[0]).getPathScannerClassLoader()
#This variable is empty by default and can be valorized by developer and it is
#included by 'class-hunter.default-path-scanner-class-loader.imports' property
class-hunter.default-path-scanner-class-loader.additional-imports=
class-hunter.default-path-scanner-class-loader.imports=\
    ${class-hunter.default-path-scanner-class-loader.additional-imports};\
    org.burningwave.core.assembler.ComponentSupplier;\
    org.burningwave.core.io.FileSystemItem;\
    org.burningwave.core.classes.PathScannerClassLoader;\
    java.util.function.Supplier;
class-hunter.default-path-scanner-class-loader.name=\
    org.burningwave.core.classes.DefaultPathScannerClassLoaderRetrieverForClassHunter
class-hunter.new-isolated-path-scanner-class-loader.search-config.check-file-option=\
    ${hunters.default-search-config.check-file-option}
hunters.default-search-config.check-file-option=\
    ${path-scanner-class-loader.search-config.check-file-option}
hunters.path-loading-lock=forPath
java-memory-compiler.class-path-hunter.search-config.check-file-option=\
    ${hunters.default-search-config.check-file-option}
path-scanner-class-loader.parent=\
    Thread.currentThread().getContextClassLoader()
path-scanner-class-loader.parent.imports=\
    ${path-scanner-class-loader.parent.additional-imports};\
    org.burningwave.core.assembler.ComponentSupplier;\
    org.burningwave.core.io.FileSystemItem;\
    org.burningwave.core.classes.PathScannerClassLoader;\
    java.util.function.Supplier;
path-scanner-class-loader.parent.name=\
    org.burningwave.core.classes.ParentClassLoaderRetrieverForPathScannerClassLoader
#other possible values are: checkFileName, checkFileName|checkFileSignature, checkFileName&checkFileSignature
path-scanner-class-loader.search-config.check-file-option=checkFileName
#This variable is empty by default and can be valorized by developer and it is
#included by 'paths.class-factory.default-class-loader.class-repositories' property
paths.class-factory.default-class-loader.additional-class-repositories=
#this variable indicates all the paths from which the classes 
#must be taken if during the definition of the compiled classes
#on classloader there will be classes not found
paths.class-factory.default-class-loader.class-repositories=\
    ${paths.java-memory-compiler.class-paths};\
    ${paths.java-memory-compiler.class-repositories};
    ${paths.class-factory.default-class-loader.additional-class-repositories}\
paths.hunters.default-search-config.paths=${paths.main-class-paths};
#This variable is empty by default and can be valorized by developer and it is
#included by 'paths.paths.java-memory-compiler.class-paths' property
paths.java-memory-compiler.additional-class-paths=
#this variable indicates all the class paths used by the JavaMemoryCompiler
#component for compiling
paths.java-memory-compiler.class-paths=\
    ${paths.main-class-paths};\
    ${paths.main-class-paths.extension};\
    ${paths.java-memory-compiler.additional-class-paths}
#This variable is empty by default and can be valorized by developer. All
#paths inserted here will analyze by JavaMemoryCompiler component before
#compiling to search for all classes imported from sources 
paths.java-memory-compiler.class-repositories=
paths.main-class-paths.extension=\
    //${system.properties:java.home}/lib//children:.*?\.jar|.*?\.jmod;\
    //${system.properties:java.home}/lib/ext//children:.*?\.jar|.*?\.jmod;\
    //${system.properties:java.home}/jmods//children:.*?\.jar|.*?\.jmod;
```
**If in your custom burningwave.properties file one of this default properties is not found, the relative default value here in the box above is assumed**.

If you create a component container instance through method ComponentContainer.create(String relativeConfigFileName), you can specify the file name of your properties file and you can locate it everywhere in your classpath project but remember to use a relative path in this case, i.e.: if you name your file "custom-config-file.properties" and put it in package "org.burningwave" you must create the component container as follow: 
```java
ComponentContainer.create("org/burningwave/custom-config-file.properties")
```
Here an example of a **burningwave.properties** file with all configurable properties:
```properties
class-factory.byte-code-hunter.search-config.check-file-option=\
    checkFileName&checkFileSignature
class-factory.default-class-loader.parent=Thread.currentThread().getContextClassLoader()
class-factory.default-class-loader=PathScannerClassLoader.create(\
    ${class-factory.default-class-loader.parent},\
    ((ComponentSupplier)parameter[0]).getPathHelper(),\
    FileSystemItem.Criteria.forClassTypeFiles(\
        FileSystemItem.CheckingOption.FOR_NAME\
    )\
)
class-factory.default-class-loader.additional-imports=java.util.function.Consumer;
class-factory.default-class-loader.imports=\
    ${class-factory.default-class-loader.additional-imports};\
    org.burningwave.core.assembler.ComponentSupplier;\
    java.util.function.Function;\
    org.burningwave.core.io.FileSystemItem;\
    org.burningwave.core.classes.PathScannerClassLoader;\
    java.util.function.Supplier;
class-factory.default-class-loader.name=\
    org.burningwave.core.classes.DefaultClassLoaderRetrieverForClassFactory
class-hunter.default-path-scanner-class-loader=\
    (Supplier<PathScannerClassLoader>)() -> ((ComponentSupplier)parameter[0]).getPathScannerClassLoader()
class-hunter.default-path-scanner-class-loader.additional-imports=java.util.function.Consumer;
class-hunter.default-path-scanner-class-loader.imports=\
    ${class-hunter.default-path-scanner-class-loader.additional-imports};\
    org.burningwave.core.assembler.ComponentSupplier;\
    org.burningwave.core.io.FileSystemItem;\
    org.burningwave.core.classes.PathScannerClassLoader;\
    java.util.function.Supplier;
class-hunter.default-path-scanner-class-loader.name=\
    org.burningwave.core.classes.DefaultPathScannerClassLoaderRetrieverForClassHunter
class-hunter.new-isolated-path-scanner-class-loader.search-config.check-file-option=\
    ${hunters.default-search-config.check-file-option}
hunters.default-search-config.check-file-option=\
    ${path-scanner-class-loader.search-config.check-file-option}
hunters.path-loading-lock=forPath
java-memory-compiler.class-path-hunter.search-config.check-file-option=\
    ${hunters.default-search-config.check-file-option}
path-scanner-class-loader.parent=\
    Thread.currentThread().getContextClassLoader()
path-scanner-class-loader.parent.imports=\
    ${path-scanner-class-loader.parent.additional-imports};\
    org.burningwave.core.assembler.ComponentSupplier;\
    org.burningwave.core.io.FileSystemItem;\
    org.burningwave.core.classes.PathScannerClassLoader;\
    java.util.function.Supplier;
path-scanner-class-loader.parent.name=\
    org.burningwave.core.classes.ParentClassLoaderRetrieverForPathScannerClassLoader
path-scanner-class-loader.search-config.check-file-option=checkFileName
paths.class-factory.default-class-loader.class-repositories=\
    ${paths.java-memory-compiler.class-paths};\
    ${paths.java-memory-compiler.class-repositories};\
    ${paths.class-factory.default-class-loader.additional-class-repositories}
paths.hunters.default-search-config.paths=${paths.main-class-paths};
paths.java-memory-compiler.class-paths=\
    ${paths.main-class-paths};\
    ${paths.main-class-paths.extension};\
    ${paths.java-memory-compiler.additional-class-paths}
paths.main-class-paths.extension=\
    //${system.properties:java.home}/lib//children:.*?\.jar|.*?\.jmod;\
    //${system.properties:java.home}/lib/ext//children:.*?\.jar|.*?\.jmod;\
    //${system.properties:java.home}/jmods//children:.*?\.jar|.*?\.jmod;
paths.java-memory-compiler.additional-class-paths=C:/some paths 1;C:/some paths 2;
paths.java-memory-compiler.class-repositories=C:/some paths 3;C:/some paths 4;
paths.class-factory.default-class-loader.additional-class-repositories=C:/some paths 5;C:/some paths 6;
```

### Other examples of using some components:
<details open>
	<summary><b>ClassFactory</b></summary>
	<ul>
		<li>
			<a href="https://github.com/burningwave/core/wiki/Generating-classes-at-runtime-and-invoking-their-methods-with-and-without-the-use-of-reflection">
			<b>USE CASE</b>: generating classes at runtime and invoking their methods with and without the use of the reflection
			</a>
		</li>
	</ul>
</details>
<details open>
	<summary><b>ClassHunter</b></summary>
	<ul>
		<li>
			<a href="https://github.com/burningwave/core/wiki/How-to-retrieve-all-classes-of-the-classpath">
			<b>USE CASE</b>: how to retrieve all classes of the classpath
			</a>
		</li>
		<li>
			<a href="https://github.com/burningwave/core/wiki/How-retrieve-all-classes-that-implement-one-or-more-interfaces">
			<b>USE CASE</b>: how to retrieve all classes that implement one or more interfaces
			</a>
		</li>
		<li>
			<a href="https://github.com/burningwave/core/wiki/Finding-all-classes-that-extend-a-base-class">
			<b>USE CASE</b>: finding all classes that extend a base class
			</a>
		</li>
		<li>
			<a href="https://github.com/burningwave/core/wiki/How-to-search-for-all-classes-that-have-package-name-that-matches-a-regex">
			<b>USE CASE</b>: how to search for all classes that have package name that matches a regex
			</a>
		</li>
		<li>
			<a href="https://github.com/burningwave/core/wiki/Finding-all-classes-for-module-name-(Java-9-and-later)">
			<b>USE CASE</b>: finding all classes for module name (Java 9 and later)
			</a>
		</li>
		<li>
			<a href="https://github.com/burningwave/core/wiki/Finding-all-annotated-classes">
			<b>USE CASE</b>: finding all annotated classes
			</a>
		</li>
		<li>
			<a href="https://github.com/burningwave/core/wiki/How-to-scan-classes-for-specific-annotations-and-collect-its-values">
			<b>USE CASE</b>: how to scan classes for specific annotations and collect its values
			</a>
		</li>
		<li>
			<a href="https://github.com/burningwave/core/wiki/How-to-search-for-all-classes-with-a-constructor-that-takes-a-specific-type-as-first-parameter-and-with-at-least-2-methods-that-begin-for-a-given-string">
			<b>USE CASE</b>: how to search for all classes with a constructor that takes a specific type as first parameter and with at least 2 methods that begin for a given string
			</a>
		</li>
		<li>
			<a href="https://github.com/burningwave/core/wiki/How-to-search-for-all-classes-with-methods-whose-name-begins-for-a-given-string-and-that-takes-a-specific-type-as-its-first-parameter">
			<b>USE CASE</b>: how to search for all classes with methods whose name begins for a given string and that takes a specific type as its first parameter
			</a>
		</li>
		<li>
			<a href="https://github.com/burningwave/core/wiki/Finding-all-classes-that-have-at-least-2-protected-fields">
			<b>USE CASE</b>: finding all classes that have at least 2 protected fields
			</a>
		</li>
	</ul>
</details>
<details open>
	<summary><b>ClassPathHunter</b></summary>
	<ul>
		<li>
			<a href="https://github.com/burningwave/core/wiki/Finding-where-a-class-is-loaded-from">
			<b>USE CASE</b>: finding where a class is loaded from
			</a>
		</li>
	</ul>
</details>
<details open>
	<summary><b>CodeExecutor</b></summary>
	<ul>
		<li>
			<a href="https://github.com/burningwave/core/wiki/Executing-stringified-source-code">
			<b>USE CASE</b>: executing stringified source code
			</a>
		</li>
	</ul>
</details>
<details open>
	<summary><b>Constructors</b></summary>
	<ul>
		<li>
			<a href="https://github.com/burningwave/core/wiki/Handling-privates-and-all-other-constructors-of-an-object">
			<b>USE CASE</b>: handling privates and all other constructors of an object
			</a>
		</li>
	</ul>
</details>
<details open>
	<summary><b>Fields</b></summary>
	<ul>
		<li>
			<a href="https://github.com/burningwave/core/wiki/Handling-privates-and-all-other-fields-of-an-object">
			<b>USE CASE</b>: handling privates and all other fields of an object
			</a>
		</li>
	</ul>
</details>
<details open>
	<summary><b>FileSystemItem</b></summary>
	<ul>
		<li>
			<a href="https://github.com/burningwave/core/wiki/Reaching-a-resource-of-the-file-system">
			<b>USE CASE</b>: reaching a resource of the file system
			</a>
		</li>
	</ul>
</details>
<details open>
	<summary><b>Methods</b></summary>
	<ul>
		<li>
			<a href="https://github.com/burningwave/core/wiki/Handling-privates-and-all-other-methods-of-an-object">
			<b>USE CASE</b>: handling privates and all other methods of an object
			</a>
		</li>
	</ul>
</details>
<details open>
	<summary><b>IterableObjectHelper</b></summary>
	<ul>
		<li>
			<a href="https://github.com/burningwave/core/wiki/Retrieving-placeholdered-items-from-map-and-properties-file">
			<b>USE CASE</b>: retrieving placeholdered items from map and properties file
			</a>
		</li>
	</ul>
</details>
<details open>
	<summary><b>PathHelper</b></summary>
	<ul>
		<li>
			<a href="https://github.com/burningwave/core/wiki/Resolving,-collecting-or-retrieving-paths">
			<b>USE CASE</b>: resolving, collecting or retrieving paths
			</a>
		</li>
	</ul>
</details>
<details open>
	<summary><b>PropertyAccessor</b></summary>
	<ul>
		<li>
			<a href="https://github.com/burningwave/core/wiki/Getting-and-setting-properties-of-a-Java-bean-through-path">
			<b>USE CASE</b>: getting and setting properties of a Java bean through path
			</a>
		</li>
	</ul>
</details>

### [**Official site**](https://www.burningwave.org/)
### [**Help guide**](https://www.burningwave.org/forum/topic/help-guide/)
### [**Ask the Burningwave community for assistance**](https://www.burningwave.org/forum/forum/how-to/)
[![HitCount](http://hits.dwyl.com/burningwave/all.svg)](http://hits.dwyl.com/burningwave/all)
<a href="https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=EY4TMTW8SWDAC&item_name=Support+maintenance+and+improvement+of+Burningwave&currency_code=EUR&source=url" rel="nofollow"><img src="https://camo.githubusercontent.com/e14c85b542e06215f7e56c0763333ef1e9b9f9b7/68747470733a2f2f7777772e70617970616c6f626a656374732e636f6d2f656e5f55532f692f62746e2f62746e5f646f6e6174655f534d2e676966" alt="Donate" data-canonical-src="https://www.paypalobjects.com/en_US/i/btn/btn_donate_SM.gif" style="max-width:100%;"></a>
