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

package org.netbeans.modules.jsrhinocontext;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.lang.model.element.TypeElement;

import javax.script.Bindings;
import javax.script.ScriptContext;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.source.ClassIndex;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.modules.jsrhinocontext.rules.RhinoCompletionRule;
import org.openide.filesystems.FileObject;


/**
 * The text-editor version of a Mozilla Rhino completion suggestor
 */
public class RhinoCompletor {
    
    public static final String FIELD_SEPARATOR = "::";
    
    /**
     * The underlying ECMAscript engine
     */
    protected ScriptEngine engine;
    
    /**
     * The object referenced in the current instruction, or null
     */
    private Object lineObj;
    
    /**
     * The classloader enable to load classes in document classpath
     */
    protected ClassLoader cld;
    
    /**
     * All the names of the available classes and interfaces in a set
     */
    TreeSet javaNames;
    
    /**
     * The set of rules to apply in automatic completion
     */
    private RhinoCompletionRule[] rules;
        
    /**
     * (Constructor) Build a completion suggestor for Rhino javascript engine derivated
     * languages
     * @param fo The <code>FileObject</code> from document where to apply autocompletion 
     */
    public RhinoCompletor(FileObject fo) {
      
        // Fill registry of java entity names
        ClassPath bootCp = ClassPath.getClassPath(fo, ClassPath.BOOT);
        ClassPath compileCp = ClassPath.getClassPath(fo, ClassPath.COMPILE);
        ClassPath sourceCp = ClassPath.getClassPath(fo, ClassPath.SOURCE);
        final ClasspathInfo info = ClasspathInfo.create(bootCp, compileCp, sourceCp);
        Set<ElementHandle<TypeElement>> entities = info.getClassIndex().getDeclaredTypes("", ClassIndex.NameKind.PREFIX, EnumSet.of(ClassIndex.SearchScope.SOURCE, ClassIndex.SearchScope.DEPENDENCIES));
        javaNames = fillJavaNames(entities);
        
        // Join classloaders on the document available classpaths
        ClassLoader bootCl = bootCp.getClassLoader(true);
        ClassLoader compileCl = compileCp.getClassLoader(true);
        ClassLoader sourceCl = sourceCp.getClassLoader(true);
        cld = new JoinClassLoader(Thread.currentThread().getContextClassLoader(), 
                bootCl, compileCl, sourceCl
        );
        Thread.currentThread().setContextClassLoader(cld);
        
        // Create an instance of the Scripting manager using the joint classloader
        ScriptEngineManager manager = new ScriptEngineManager(cld);
        
        // Get the reference to the rhino scripting engine.
        engine = manager.getEngineByName("javascript"); 
    }
       
    /**
     * @param entities The available Java entities from the classpath
     * @param buffer The input buffer
     * @param cursor The current cursor position
     * @return All the names of the available classes and interfaces in a set
     */
    private TreeSet fillJavaNames(Set<ElementHandle<TypeElement>> entities) {        
        String[] names = new String[entities.size()];
        int count = 0;
        for (ElementHandle<TypeElement> ntt : entities) {
            names[count++] = ntt.getQualifiedName();
        }
        
        return new TreeSet(Arrays.asList(names));
    }
    
    /**
     * @return The underlying script engine
     */
    public ScriptEngine getEngine() {
        return engine;
    }
    
    /**
     * Set a different ScriptEngine for the underlying evaluations
     */
    public void setEngine(ScriptEngine engine) {
        this.engine = engine;
    }
    
    /**
     * @return The script context bindings from the engine scope
     */
    public Bindings getEngineBindings() {
        ScriptContext context = engine.getContext();
        return context.getBindings(ScriptContext.ENGINE_SCOPE);
    }
    
    /**
	 * Set the engine scope script context bindings
	 * @param bindings The bindings to set
	 */
	public void setEngineBindings(Bindings bindings) {
		engine.setBindings( bindings, ScriptContext.ENGINE_SCOPE);
	}

    
    /**
     * @return The names of the java entities in the classpath in a sorted set
     */
    public TreeSet getJavaNames() {
        return javaNames;
    }
    
    /**
     * @return The object identified as current invocation target, or null
     */
    public Object getLineObject() {
        return lineObj;
    }
    
    /**
     * Set the object referenced by the current instruction line, called
     * by any rule that manages to identify it
     * @param The object identified as current invocation target, or null
     */
    public void setLineObject(Object lineObj) {
        this.lineObj = lineObj;
    }
    
    /**
     * @return The set of rules to apply in automatic completion
     */
    public RhinoCompletionRule[] getRules() {
        return rules;
    }
    
    /**
     * @param The set of rules to apply in automatic completion. Matching order 
     * matters. The rules are applyed FIFO
     */
    public void setRules(RhinoCompletionRule[] rules) {
        this.rules = rules;
    }
    

    /**
     * Fill the <code>candidates</code> list with the possible completion strings
     * @param buffer The current line
     * @param cursor The cursor position at the current line
     * @param candidates The list of candidates for the autocompletion
     * @return the number of characters on the current line that partially matched
     * the available completion candidates
     * @throws ScriptException If an error occurs while evaluating Rhino code
     */
    public int complete(String buffer, int cursor, List<String> candidates) throws ScriptException {
        // Offset the buffer to last space character,
        // so that auto-completor is available not only at buffer start
        // but every key point
        int cursorOffset = 0;
        String bufferOffset = "";
        if (buffer.contains(" ") || buffer.contains("(")) {
            int index = Math.max(buffer.lastIndexOf(" "),
                    buffer.lastIndexOf("("));
            bufferOffset = buffer.substring(0, index + 1);
            String resetBuffer = buffer.substring(bufferOffset.length());
            cursorOffset = buffer.length() - resetBuffer.length();
            cursor = cursor - cursorOffset;
            buffer = resetBuffer;
        }

        String matchedPart = "";
        lineObj = null;
        for(RhinoCompletionRule rule : rules) {
            matchedPart += rule.match(bufferOffset, buffer, cursor, candidates);
        }
        
        return bufferOffset.length() + buffer.length()
                    - matchedPart.length();
    }
}
