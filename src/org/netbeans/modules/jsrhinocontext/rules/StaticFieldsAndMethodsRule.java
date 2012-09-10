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
import java.lang.reflect.Modifier;
import java.util.List;
import org.netbeans.modules.jsrhinocontext.RhinoCompletor;

/**
 * Tries to match last part of the input buffer to a static method invokation
 * @author adriano
 */
public class StaticFieldsAndMethodsRule extends RhinoCompletionRule {
    
    public StaticFieldsAndMethodsRule(RhinoCompletor completor) {
        super(completor);
    }

    @Override
    public String match(String bufferOffset, String buffer, int cursor, List<String> candidates) {
        Object lineObj = completor.getLineObject();
        if (lineObj == null) {
            String[] parts = getMatchParts(buffer, cursor);
            String lastPart = getLastMatchPart(buffer, parts);
            if (parts.length >= 2
                    && parts[parts.length - 2].charAt(0) == parts[parts.length - 2].toUpperCase().charAt(0)) {
                try {
                    String className = buffer.substring(0,
                            buffer.lastIndexOf("."));
                    lineObj = loadClass(className);
                    // It's important to set the reference object so that other
                    // rules won't trigger
                    completor.setLineObject(lineObj);
                    if (lineObj != null) {
                        for (Field field : ((Class<?>) lineObj).getFields()) {
                            if (Modifier.isStatic(field.getModifiers())) {
                                if (lastPart.equals("")
                                        || field.getName().startsWith(lastPart)) {
                                    addCandidate(candidates, className + "." + field.getName(), 
                                            "Static field", field.toGenericString());
                                }
                            }
                        }
                        for (Method meth : ((Class<?>) lineObj).getMethods()) {
                            if (Modifier.isStatic(meth.getModifiers())) {
                                if (lastPart.equals("")
                                        || meth.getName().startsWith(lastPart)) {
                                    Class<?>[] params = meth.getParameterTypes();
                                    parameterComplete(
                                            className + "." + meth.getName(),
                                            params, candidates, "Static method", meth.toGenericString());
                                }
                            }
                        }
                    }
                    if (!lastPart.equals("")) {
                        return className + lastPart;
                    } else {
                        return buffer;
                    }
                } catch (ClassNotFoundException e) {
                    // Don't trace exception. This just isn't a static method
                    // invocation and we only know it by now
                }
            }
        }
        return "";
    }
    
}
