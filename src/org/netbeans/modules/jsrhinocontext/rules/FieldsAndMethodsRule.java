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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import javax.script.Bindings;
import org.netbeans.modules.jsrhinocontext.RhinoCompletor;
import org.openide.util.Exceptions;

/**
 * Tries to match last part of the input buffer to an object and suggest the names
 * of its fields and methods
 * @author adriano
 */
public class FieldsAndMethodsRule extends RhinoCompletionRule {
    
    public FieldsAndMethodsRule(RhinoCompletor completor) {
        super(completor);
    }
    
    @Override
    public String match(String bufferOffset, String buffer, int cursor, List<String> candidates) {
        Object lineObj = completor.getLineObject();
        String[] parts = getMatchParts(buffer, cursor);
        Bindings bindings = completor.getEngineBindings();
        String lastPart = getLastMatchPart(buffer, parts);

        if (lineObj == null) {
            String objectName = "";
            if (!bufferOffset.endsWith("(")) {
                // This will autocomplete fields and methods of objects in scope
                if (parts.length > 1
                        && bindings.containsKey(parts[parts.length - 2])) {
                    objectName = parts[parts.length - 2];
                    lineObj = bindings.get(objectName);
                    completor.setLineObject(lineObj);
                }
                if (lineObj != null) {
                    Class<?> clazz = lineObj.getClass();
                    if (clazz.getSimpleName().equals("NativeObject")) {
                        // Get native java object fields and methods
                        try {	
                            Object[] ids = NativeResolver.getIds(lineObj);
                            for(Object id : ids) {
                                String key = (String) id; 
                                try {
                                    if ((Boolean) completor.getEngine().eval(
                                            "typeof this." + objectName + "." + key + " === 'function'")) {
                                        key += "( ";
                                    }
                                } catch (Exception e) {
                                    Exceptions.printStackTrace(e);
                                }
                                String candidate = objectName + "." + key;
                                if (key.indexOf(lastPart) == 0) {
                                    if(candidate.endsWith("(")) {
                                        addCandidate(candidates, candidate, "Method", "(Native object)");
                                    } else {
                                        addCandidate(candidates, candidate, "Field", "(Native object)");
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Exceptions.printStackTrace(e);
                        }
                    } else {
                        // If an object is matched, use reflection to autocomplete
                        // further
                        Class objClass = lineObj.getClass();
                        Field[] fields = objClass.getFields();
                        for (Field field : fields) {
                            String name = field.getName();
                            if (name.startsWith(lastPart)) {
                                addCandidate(candidates, objectName + "." + name, "Field", objClass.getCanonicalName());
                            }
                        }
                        Method[] meths = objClass.getMethods();
                        for (Method meth : meths) {
                            String name = meth.getName();
                            if (name.startsWith(lastPart)) {
                                addCandidate(candidates, objectName + "." + name + "(", "Method", objClass.getCanonicalName());
                            }
                        }
                    }
                }
                if (!objectName.equals("")) {
                    return objectName + "." + lastPart;
                } else if (bufferOffset.length() > 0) {
                    return objectName;
                }
            }
        } else if (!lastPart.equals("")) {
            return lastPart;
        }
        return "";
    }
    
}
