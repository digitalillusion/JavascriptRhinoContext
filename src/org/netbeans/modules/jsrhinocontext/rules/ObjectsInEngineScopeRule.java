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

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.script.Bindings;
import org.netbeans.modules.jsrhinocontext.RhinoCompletor;
import org.openide.util.Exceptions;

/**
 * Tries to match last part of the input buffer to any name of object or function
 * in the engine scope
 * @author adriano
 */
public class ObjectsInEngineScopeRule extends RhinoCompletionRule {
    
    public ObjectsInEngineScopeRule(RhinoCompletor completor) {
        super(completor);
    }
    
    private boolean isClassName(String candidate, String buffer) {
        if(candidate.equals(buffer)) {
            return true;
        }
        if(candidate.endsWith("." + buffer) && !buffer.equals("")) {
            return true;
        }
        
        if(!candidate.replace(buffer, "").contains(".")) {
            try {
                loadClass(candidate);
                return true;
            } catch (ClassNotFoundException ex) {
                return false;
            }
        }
        return false;
    }
    
     /**
     * Tries to match last part of the input buffer to any name of class in the 
     * classpath
     * @param buffer The input buffer
     * @param cursor The current cursor position
     * @param candidates the current list of autocompletion candidates
     */
    private void matchClassName(String buffer, int cursor,
            List<String> candidates) {
        List<String> classCandidates = new ArrayList<String>();
        buffer = (buffer == null) ? "" : buffer;
        for (Iterator i = completor.getJavaNames().iterator(); i.hasNext();) {
            String candidate = (String) i.next();
            boolean isClassName = isClassName(candidate, buffer);
            if (candidate.startsWith(buffer) || isClassName) {
                if(!isClassName) {
                    int index = candidate.indexOf(".", cursor);
                    if (index != -1) {
                        candidate = candidate.substring(0, index + 1);
                    }
                }
                // Filter dupes
                if (!classCandidates.contains(candidate)) {
                    classCandidates.add(candidate);
                }
            }
        }

        // Match java entities to class names
        for (String candidate : classCandidates) {
            // Proceed if a full qualifed class name was matched
            candidate = candidate.trim();
            if (isClassName(candidate, buffer)) {
                try {
                    Class<?> clazz = loadClass(candidate);
                    if (clazz.isInterface()) {
                        addCandidate(candidates, candidate + "(", "Interface", clazz.getCanonicalName());
                    } else {
                        addCandidate(candidates, clazz.getName(), "Class", clazz.getCanonicalName());

                        // Treat also constructors as candidates
                        Constructor<?>[] ctors = clazz.getConstructors();
                        for (Constructor<?> ctor : ctors) {
                            Class<?>[] params = ctor.getParameterTypes();
                            parameterComplete(ctor.getName(), params,
                                    candidates, "Constructor", ctor.toGenericString());
                        }
                    }
                } catch (Throwable t) {
                    Exceptions.printStackTrace(t);
                }
            } else {
                addCandidate(candidates, candidate, "Package", candidate.substring(0, candidate.length()-1));
            }
        }

    }
    
    @Override
    public String match(String bufferOffset,
            String buffer, int cursor, List<String> candidates) {
        Object lineObj = completor.getLineObject();
        if (lineObj == null) {
            String[] parts = getMatchParts(buffer, cursor);
            Bindings bindings = completor.getEngineBindings();
            String lastPart = getLastMatchPart(buffer, parts);

            // This will match class names
            matchClassName(buffer, cursor, candidates);

            if (parts.length <= 1 && !bufferOffset.trim().endsWith("new")   ) {
                // Let bindings appear among candidates at engine scope
                // Only if there is no instancing currently going on
                Set<String> keySet = new HashSet<String>();
                // Store key set locally to avoid concurrent modification
                for (String key : bindings.keySet()) {
                    keySet.add(key);
                }
                for (String key : keySet) {
                    String desc = "Object";
                    try {
                        if ((Boolean) completor.getEngine().eval("typeof this." + key
                                + " === 'function'")) {
                            key += "(";
                            desc = "Function";
                        }
                    } catch (Exception e) {
                        Exceptions.printStackTrace(e);
                    }
                    if (key.indexOf(lastPart) == 0) {
                        addCandidate(candidates, key, desc, "(Engine scope)");
                    }
                }
            }
            return buffer;
        }
        return "";
    }
}
