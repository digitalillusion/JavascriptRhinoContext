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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import javax.script.Bindings;
import org.netbeans.modules.jsrhinocontext.RhinoCompletor;

/**
 * Base class for Rhino completion rules
 * @author adriano
 */
public abstract class RhinoCompletionRule {
    
    /**
     * Reference to the main completor object
     */
    protected final RhinoCompletor completor;
    
    public RhinoCompletionRule(RhinoCompletor completor) {
        this.completor = completor;
    }
    
    /**
     * The rule match method
     * @param bufferOffset The part of the buffer that has already been autocompleted
     * @param buffer The input buffer
     * @param cursor The current cursor position
     * @param candidates the current list of autocompletion candidates
     * @return the matched part of the input buffer
     */
    public abstract String match(String bufferOffset, String buffer,
            int cursor, List<String> candidates);
    
    /**
     * Complete possible method signatures trying to guess parameter names
     * @param methodName The name of the method under question
     * @param params The method parameters
     * @param candidates The completion candidates list
     * @param description The description of the candidate
     * @param context the documentation context to add to the candidate
     */
    protected void parameterComplete(String methodName, Class<?>[] params,
            List<String> candidates, String description, String context) {
        String candidate = methodName + "(";

        Bindings bindings = completor.getEngineBindings();
        Collection<String> bindingsKeys = bindings.keySet();
        // Map each type to a list of objects of the same type from the
        // bindings, using keys
        HashMap<String, ArrayList<String>> typeMap = new HashMap<String, ArrayList<String>>();
        for (String key : bindingsKeys) {
            if (bindings.get(key) != null) {
                String typeMapKey = bindings.get(key).getClass().getName();
                if (typeMap.containsKey(typeMapKey)) {
                    typeMap.get(typeMapKey).add(key);
                } else {
                    ArrayList<String> list = new ArrayList<String>();
                    list.add(key);
                    typeMap.put(typeMapKey, list);
                }
            }
        }
        ArrayList<String> proposals = new ArrayList<String>();
        for (int i = 0; i < params.length; i++) {
            String defaultParam = params[i].getName();
            candidate = makeCommaSepList(i, candidate);

            String key = params[i].getName();
            if (typeMap.containsKey(key)) {
                ArrayList<String> matchingBindKeys = typeMap.get(key);
                // Add a completion proposal for each matching bindings key
                for (String bindKey : matchingBindKeys) {
                    // Update current proposals adding the matched parameter
                    for (int j = 0; j < proposals.size(); j++) {
                        String proposal = proposals.get(j);
                        boolean propCompleted = proposal.split(",").length <= i;
                        if (propCompleted) {
                            proposal = makeCommaSepList(i, proposal);
                            proposal += bindKey;
                            proposals.set(j, proposal);
                        }
                    }
                    proposals.add(candidate + bindKey);
                }
            } else {
                // Update current proposals adding a default parameter
                for (int j = 0; j < proposals.size(); j++) {
                    String proposal = proposals.get(j);
                    proposal = makeCommaSepList(i, proposal);
                    proposal += defaultParam;
                    proposals.set(j, proposal);
                }
            }
            candidate += defaultParam;
        }
        // Add default candidate to proposals and fix spacing
        proposals.add(candidate);
        for (String proposal : proposals) {
            proposal += ")";
            addCandidate(candidates, proposal, description, context);
        }
    }
    
    
    /**
     * Split input buffer into several parts using "." (the dot) as 
     * delimiter. It is used to identify method calls, object properties, 
     * etc.
     * @param buffer The input buffer
     * @param cursor The current cursor position
     * @return An array of strings containing the buffer split at each 
     * delimiter
     */
    protected String[] getMatchParts(String buffer, int cursor) {
        // Starting from "cursor" at the end of the buffer, look backward
        // and collect a list of identifiers separated by (possibly zero)
        // dots. Then look up each identifier in turn until getting to the
        // last, presumably incomplete fragment. Then enumerate all the
        // properties of the last object and find any that have the
        // fragment as a prefix and return those for autocompletion.
        int m = cursor - 1;
        while (m >= 0) {
            char c = buffer.charAt(m);
            if (!Character.isJavaIdentifierPart(c) && c != '.' && c != '(') {
                break;
            }
            m--;
        }
        if (m + 1 >= 0 && cursor >= 0) {
            String namesAndDots = buffer.substring(m + 1, cursor);
            String[] names = namesAndDots.split("\\.", -1);
            ArrayList<String> parts = new ArrayList<String>();
            for (String part : names) {
                if (!part.equals("")) {
                    parts.add(part);
                }
            }
            if (namesAndDots.endsWith(".")) {
                parts.add("");
            }
            return parts.toArray(new String[parts.size()]);
        }
        return new String[]{buffer};
    }
        
    /**
     * Once the buffer has been divided into parts using {@link getMatchParts()},
     * it is possible to identify the most significant part, the last, for further
     * elaborations
     * @param buffer The input buffer
     * @param parts The parts of the input buffer
     * @return The last of the parts of the input buffer
     * @see RhinoCompletor.getMatchParts
     */
    protected String getLastMatchPart(String buffer, String[] parts) {
        String lastPart = buffer;
        if (parts.length == 1) {
            lastPart = parts[0];
        }
        if (parts.length > 1) {
            lastPart = parts[parts.length - 1];
        }
        return lastPart;
    }
    
    /**
     * Used in a loop, append a comma to the <code>buffer</code>
     * when <code>index</code> is higher than 0
     * @param index The loop index
     * @param buffer The buffer where concatenation occurs
     * @return The same object as the input parameter <code>buffer</code>
     * but possibly with a ", " (comma, space) appended
     */
    protected String makeCommaSepList(int index, String buffer) {
        if (index > 0) {
            buffer += ", ";
        }
        return buffer;
    }
    
    /**
     * Wrapper function to add candidates to the list including some meta info
     * @param candidates The candidate list
     * @param element The candidate to add
     * @param type The description of the type of the lement
     * @param context Information about the context of the call
     */
    protected void addCandidate(List<String> candidates, String element, String type, String context) {
        String def = element + 
                RhinoCompletor.FIELD_SEPARATOR + type +
                RhinoCompletor.FIELD_SEPARATOR + context; 
        candidates.add(def);
    }
    
    /**
     * Wrapper function to load classes by mean of the current class loader
     * @param className The fully qualified name of the class to load
     * @return The class corresponding to the given name
     * @throws ClassNotFoundException  if the <code>className</code> is not
     * resolvable
     */
    protected Class loadClass(String className) throws ClassNotFoundException {
        try {
            return Thread.currentThread().getContextClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {};
        
        for (Iterator i = completor.getJavaNames().iterator(); i.hasNext();) {
            String candidate = (String) i.next();
            if (candidate.endsWith("." + className)) {
                return Thread.currentThread().getContextClassLoader().loadClass(candidate);
            }
        }
        throw new ClassNotFoundException(className);
        
    }
    
}
