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

import java.lang.reflect.Method;
import java.util.List;
import org.netbeans.modules.jsrhinocontext.RhinoCompletor;

/**
 * Tries to match last part of the input buffer to a method call and display
 * possible paramters autocompletion
 *
 * @author adriano
 */
public class MethodSignatureRule extends RhinoCompletionRule {
    
    public MethodSignatureRule(RhinoCompletor completor) {
        super(completor);
    }
    
    @Override
    public String match(String bufferOffset, String buffer, int cursor, List<String> candidates) {
        Object lineObj = completor.getLineObject();
        if (lineObj == null) {
            int startSel;
            String lastPart = bufferOffset.trim();
            if (lastPart.contains(" ")) {
                lastPart = lastPart.substring(lastPart.lastIndexOf(" ") + 1);
            }
            if (lastPart.endsWith("(") && buffer.equals("")
                    && (startSel = lastPart.lastIndexOf(".")) > 0) {
                try {
                    String objectName = lastPart.substring(0, startSel);
                    String methodName = lastPart.substring(startSel + 1,
                            lastPart.length() - 1);
                    Object object = completor.getEngineBindings().get(objectName);
                    if (object == null) {
                        // Object is not in scope, so it must be a class that is
                        // declaring some static method
                        object = loadClass(objectName);
                    }
                    Method[] methods = object.getClass().getMethods();
                    // Autocomplete the possible method signatures
                    for (Method meth : methods) {
                        if (meth.getName().equals(methodName)) {
                            Class<?>[] params = meth.getParameterTypes();
                            parameterComplete(
                                    objectName + "." + meth.getName(), params,
                                    candidates, "Method", meth.toGenericString());
                        }
                    }

                    // So that other autocompletions are not triggered
                    completor.setLineObject(new Object());

                    return objectName + "." + methodName + "(";
                } catch (Exception e) {
                    // Last part is not a method name; it might be the class
                    // name itself, if this is a default constructor.
                }
            }
        }
        return "";
    }
    
}
