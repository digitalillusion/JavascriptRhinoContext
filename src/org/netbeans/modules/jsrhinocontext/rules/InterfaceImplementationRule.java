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
import java.util.ArrayList;
import java.util.List;
import org.netbeans.modules.jsrhinocontext.RhinoCompletor;

/**
 * Tries to suggest the implementation of the interfaces instanced inline
 * @author adriano
 */
public class InterfaceImplementationRule extends RhinoCompletionRule {
    
    public InterfaceImplementationRule(RhinoCompletor completor) {
        super(completor);
    }

    @Override
    public String match(String bufferOffset, String buffer, int cursor, List<String> candidates) {
        Object lineObj = completor.getLineObject();
        if (lineObj == null) {
            String lastPart = bufferOffset.trim();
            if (lastPart.contains(" ")) {
                lastPart = lastPart.substring(lastPart.lastIndexOf(" ") + 1);
            }

            if (lastPart.endsWith("(")) {
                try {
                    String objectName = lastPart.substring(0,
                            lastPart.length() - 1);
                    Class<?> object = loadClass(objectName);
                    Method[] methods = object.getMethods();
                    if (object.isInterface()) {
                        // Interface is instantiated directly
                        String candidate = "({";
                        for (int j = 0; j < methods.length; j++) {
                            Method meth = methods[j];
                            candidate = makeCommaSepList(j, candidate);
                            candidate += "\n\t" + meth.getName()
                                    + ": function (";
                            Class<?>[] params = meth.getParameterTypes();
                            ArrayList<String> paramNames = new ArrayList<String>();
                            for (int i = 0; i < params.length; i++) {
                                candidate = makeCommaSepList(i, candidate);
                                String paramName = params[i].getName();
                                String simpleName = paramName.substring(paramName.lastIndexOf(".") + 1);
                                if(params[i].isArray()) {
                                    simpleName = paramName.substring(paramName.lastIndexOf(".") + 1);
                                    simpleName = simpleName.substring(0, simpleName.length()-1);
                                    simpleName += "Array";
                                }
                                paramName = simpleName.substring(0, 1).toLowerCase()
                                        + simpleName.substring(1);
                                // Disambiguate duplicate names
                                if (paramNames.contains(paramName)) {
                                    paramName += "I";
                                }
                                paramNames.add(paramName);
                                candidate += paramName;
                            }
                            candidate += ") { }";
                        }
                        candidate += "\n})";
                        addCandidate(candidates, candidate, "Interface implementation", object.getCanonicalName());
                    }

                    // So that other autocompletions are not triggered
                    completor.setLineObject(new Object());
                    return lastPart.substring(lastPart.indexOf("("));
                } catch (Exception e) {
                    // Last part is not an interface name
                }
            }
        }
        return "";
    }
    
}
