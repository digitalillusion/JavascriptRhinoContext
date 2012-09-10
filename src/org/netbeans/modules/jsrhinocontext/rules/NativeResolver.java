/*
 *                 Sun Public License Notice
 *
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 *
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2005 Sun
 * Microsystems, Inc. All Rights Reserved.
 */
package org.netbeans.modules.jsrhinocontext.rules;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class NativeResolver {
	
	/**
	 * Traverse an object class hierarchy to resolve one class given its name
	 * 
	 * @param obj The object in question
	 * @param className The name of the class to look for
	 * @return a class from the object class hierarchy
	 * @throws ClassNotFoundException If the object is not instance of <code>className</code>
	 */
	private static Class<?> getObjectClass(Object obj, String className) throws ClassNotFoundException {
		Class<?> clazz = obj.getClass();
		Class<?> inheritor = clazz;
		// Traverse class hierarchy to resolve ScriptableObject
		while(!inheritor.getSimpleName().equals(className) &&
			  !inheritor.getSimpleName().equals("Object")) {
			inheritor = inheritor.getSuperclass();
		}
		if(!inheritor.getSimpleName().equals(className)) {
			throw new ClassNotFoundException();
		}
		return inheritor;
	}
	
	/**
	 * Invokes a method called <code>methodName</code> on an object
	 * 
	 * @param scriptableObject The object in question
	 * @param className The name of the class that defines the method
	 * @param methodName The name of the method to call
	 * @param paramTypes An array specifying the types of the method parameters
	 * @param paramsValues An array specifying the values of the method parameters
	 * @return The method return result as an Object
	 * @throws ClassNotFoundException If the object is not instance of ScriptableObject
	 * @throws NoSuchMethodException If the method does not exist
	 * @throws SecurityException If reflection fails invocation
	 * @throws IllegalArgumentException If reflection fails invocation
	 * @throws IllegalAccessException If reflection fails invocation
	 * @throws InvocationTargetException If reflection fails invocation
	 * 
	 * @see java.lang.reflect.Method#invoke(Object, Object...)
	 */
	public static Object invoke(Object object, String className, String methodName, Class<?>[] paramTypes, Object[] paramsValues ) throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		// Invocation
		Method m = getObjectClass(object, className).getMethod(methodName, paramTypes);
		Object returnValue = m.invoke(object, paramsValues);
		return returnValue;
	}
	
	/**
	 * Invokes the getIds() method on a scriptable object
	 * 
	 * @param scriptableObject The object in question
	 * @return The array of ids
	 * @throws ClassNotFoundException If the object is not instance of ScriptableObject
	 * @throws NoSuchMethodException If the method does not exist
	 * @throws SecurityException If reflection fails invocation
	 * @throws IllegalArgumentException If reflection fails invocation
	 * @throws IllegalAccessException If reflection fails invocation
	 * @throws InvocationTargetException If reflection fails invocation
	 * 
	 * @see java.lang.reflect.Method#invoke(Object, Object...)
	 */
	public static Object[] getIds(Object scriptableObject) throws SecurityException, IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		return (Object[]) NativeResolver.invoke(
				scriptableObject, 
				"ScriptableObject",
				"getIds", 
				new Class[] {}, 
				new Object[] {}
			);
	}
	
	/**
	 * Invokes the get() method on a scriptable object
	 * 
	 * @param scriptableObject The object in question
	 * @param property The property to get
	 * @return The property value
	 * @throws ClassNotFoundException If the object is not instance of ScriptableObject
	 * @throws NoSuchMethodException If the method does not exist
	 * @throws SecurityException If reflection fails invocation
	 * @throws IllegalArgumentException If reflection fails invocation
	 * @throws IllegalAccessException If reflection fails invocation
	 * @throws InvocationTargetException If reflection fails invocation
	 * 
	 * @see java.lang.reflect.Method#invoke(Object, Object...)
	 */
	public static Object get(Object scriptableObject, String property) throws SecurityException, IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		Object result = (Object) NativeResolver.invoke(
			scriptableObject,
			"ScriptableObject",
			"get", 
			new Class[] { String.class, getInterface(scriptableObject, "ScriptableObject", "Scriptable") }, 
			new Object[] { property, scriptableObject }
		);

		if (result.getClass().getSimpleName().equals("NativeArray")) {
			result = getArray(result);
		}
		return result;
	}
	
	/**
	 * Return a NativeArray as an array of Java objects
	 * 
	 * @param nativeArray The native array in question
	 * @return An array of Java objects
	 * @throws ClassNotFoundException If the object is not instance of ScriptableObject
	 * @throws NoSuchMethodException If the method does not exist
	 * @throws SecurityException If reflection fails invocation
	 * @throws IllegalArgumentException If reflection fails invocation
	 * @throws IllegalAccessException If reflection fails invocation
	 * @throws InvocationTargetException If reflection fails invocation
	 * 
	 * @see java.lang.reflect.Method#invoke(Object, Object...)
	 */
	public static Object[] getArray(Object nativeArray) throws SecurityException, IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		long len = (Long) invoke(
			nativeArray, 
			"NativeArray",
			"getLength", 
			new Class[] {}, 
			new Object[] {}
		);
		ArrayList<Object> objects = new ArrayList<Object>();
		for (int i = 0; i < len; i++) {
			Object o = (Object) NativeResolver.invoke(
					nativeArray,
					"NativeArray",
					"get", 
					new Class[] { Integer.TYPE, getInterface(nativeArray, "ScriptableObject", "Scriptable") }, 
					new Object[] { i, nativeArray }
				);
			if (o.getClass().getSimpleName().equals("NativeArray")) {
				o = getArray(o);
			}
			objects.add(o);
		}
		return objects.toArray(new Object[objects.size()]);
	}

	/**
	 * Invokes the get() method on a scriptable object, providing a default value
	 * 
	 * @param scriptableObject The object in question
	 * @param property The property to get
	 * @param defaultValue The default return value in case of error
	 * @return The property value if possible, the default value otherwise
	 * 
	 * @see java.lang.reflect.Method#invoke(Object, Object...)
	 */
	public static Object get(Object scriptableObject, String property, Object defaultValue) {
		try {
			Class<?> defaultClass = defaultValue.getClass();
			Object result = NativeResolver.invoke(
					scriptableObject, 
					"ScriptableObject",
					"get", 
					new Class[] { String.class, getInterface(scriptableObject, "ScriptableObject", "Scriptable") }, 
					new Object[] { property, scriptableObject }
				);
			return defaultClass.cast(result);
		} catch (Exception e) {
			return defaultValue;
		}
	}

	/**
	 * Traverse an object class hierarchy to resolve an interface
	 * 
	 * @param obj The object in question
	 * @param implementorClassName The class from the <code>obj</code> class hierarchy
	 * that should implement the interface
	 * @param interfaceName The interface to look for
	 * @return an interface from the ones implemented in the class <code>implementorClassName</code>
	 * by the object <code>obj</code>
	 * @throws ClassNotFoundException If the object does not implement the given interface
	 */
	private static Class<?> getInterface(Object obj, String implementorClassName, String interfaceName) throws ClassNotFoundException {
		Class<?> clazz = getObjectClass(obj, implementorClassName);
		Class<?>[] interfaces = clazz.getInterfaces();
		for (Class<?> intf : interfaces) {
			if(intf.getSimpleName().equals(interfaceName)) {
				return intf;
			}			
		}
		throw new ClassNotFoundException();
	}
}
