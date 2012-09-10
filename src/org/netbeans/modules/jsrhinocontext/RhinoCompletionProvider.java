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

import java.util.ArrayList;
import java.util.Iterator;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.EditorKit;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyledDocument;
import org.netbeans.modules.jsrhinocontext.rules.FieldsAndMethodsRule;
import org.netbeans.modules.jsrhinocontext.rules.InterfaceImplementationRule;
import org.netbeans.modules.jsrhinocontext.rules.MethodSignatureRule;
import org.netbeans.modules.jsrhinocontext.rules.ObjectsInEngineScopeRule;
import org.netbeans.modules.jsrhinocontext.rules.RhinoCompletionRule;
import org.netbeans.modules.jsrhinocontext.rules.StaticFieldsAndMethodsRule;
import org.netbeans.modules.jsrhinocontext.ui.RhinoEvalResultTopComponent;
import org.netbeans.spi.editor.completion.CompletionProvider;
import org.netbeans.spi.editor.completion.CompletionResultSet;
import org.netbeans.spi.editor.completion.CompletionTask;
import org.netbeans.spi.editor.completion.support.AsyncCompletionQuery;
import org.netbeans.spi.editor.completion.support.AsyncCompletionTask;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.text.CloneableEditorSupport;
import org.openide.util.Exceptions;
import org.openide.windows.TopComponent;
import org.openide.windows.TopComponent.Registry;

/**
 *
 * @author Adriano
 */
public class RhinoCompletionProvider implements CompletionProvider {
    private boolean enabled;

    @Override
    public CompletionTask createTask(int i, JTextComponent jTextComponent) {
        boolean completionEnabled = isEnabled();
        
        if (!completionEnabled || i != CompletionProvider.COMPLETION_QUERY_TYPE) {
            if(!completionEnabled) {
                updateResultWindow(null, "Rhino code assist is disabled");
            }
            return null;
        }

        return new AsyncCompletionTask(new AsyncCompletionQuery() {

            @Override
            protected void query(CompletionResultSet completionResultSet, Document document, int caretOffset) {
               
                Object sdp = document.getProperty(Document.StreamDescriptionProperty);
                FileObject fo = null;
                if (sdp instanceof FileObject) {
                    fo = (FileObject) sdp;
                }
                if (sdp instanceof DataObject) {
                    DataObject dobj = (DataObject) sdp;
                    fo = dobj.getPrimaryFile();
                }
                try {
                    final StyledDocument bDoc = (StyledDocument) document;
                    final int wordStartOffset = getRowFirstNonWhite(bDoc, caretOffset);
                    boolean evalEx = false;
                    if (wordStartOffset >= 0) {
                        int inlineCaretOffset = caretOffset - wordStartOffset;
                        final String buffer = bDoc.getText(wordStartOffset, inlineCaretOffset);
                        final int cursor = inlineCaretOffset;
                        ArrayList<String> candidates = new ArrayList<String>();
                        
                        // Setup the autocompletor
                        String script = bDoc.getText(0, caretOffset - buffer.length());
                        RhinoCompletor completor = new RhinoCompletor(fo);
                        RhinoCompletionRule[] rules = new RhinoCompletionRule[5];
                        rules[0] = (RhinoCompletionRule) new MethodSignatureRule(completor);
                        rules[1] = (RhinoCompletionRule) new StaticFieldsAndMethodsRule(completor);
                        rules[2] = (RhinoCompletionRule) new FieldsAndMethodsRule(completor);
                        rules[3] = (RhinoCompletionRule) new InterfaceImplementationRule(completor);
                        rules[4] = (RhinoCompletionRule) new ObjectsInEngineScopeRule(completor);
                        
                        // Allow further initialization
                        completor.setEngine(initEngine(completor.getEngine()));
                        completor.setRules(initRules(rules, completor));
                        
                        try {
                            ScriptEngine engine = completor.getEngine();
                            Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
                            engine.eval(script, bindings);
                            bindings.remove("context");
                        } catch (ScriptException ex) {
                            updateResultWindow(fo, ex.getMessage());
                            evalEx = true;
                        }

                        // Finally, perform autocompletion and fill result set
                        int matchOffset = completor.complete(buffer, cursor, candidates);
                        for(String candidate : candidates) {
                            completionResultSet.addItem(new RhinoCompletionItem(candidate, wordStartOffset + matchOffset, caretOffset));
                        }
                    }  
                    
                    if(!evalEx) {
                        updateResultWindow(fo, "Parsed successfully.");
                    }
                } catch (Exception ex) {
                    Exceptions.printStackTrace(ex);
                }
                completionResultSet.finish();
            };
        }, jTextComponent);

    }

    static int getRowFirstNonWhite(StyledDocument doc, int offset)
            throws BadLocationException {
        Element lineElement = doc.getParagraphElement(offset);
        int start = lineElement.getStartOffset();
        while (start + 1 < lineElement.getEndOffset()) {
            try {
                if (doc.getText(start, 1).charAt(0) != ' ') {
                    break;
                }
            } catch (BadLocationException ex) {
                throw (BadLocationException) new BadLocationException(
                        "calling getText(" + start + ", " + (start + 1) +
                        ") on doc of length: " + doc.getLength(), start).initCause(ex);
            }
            start++;
        }
        return start;
    }

    @Override
    public int getAutoQueryTypes(JTextComponent jtc, String string) {
        return 0;
    }
    
    /**
     * Switch the enablement of this completion provider on or off
     */
    public void toggleEnable() {
        enabled = !enabled;
    }

    /**
     * @return True if the completion provider is enabled, false otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    
    /**
     * Used when extender modules need to initialize the script engine
     * to perform their own behaviour. The default implementation does nothing
     * @param engine The engine that will be used to parse the document
     * @param script The script that will be evaluated
     * @return The default implementation returns the input parameter
     */ 
    protected ScriptEngine initEngine(ScriptEngine engine) {
        return engine;
    }
    
    /**
     * Used when extender modules need to initialize the completion rules
     * to perform their own behaviour. The default implementation does nothing
     * @param rules The rules that will be used for completion
     * @param completor The completor that will apply the rules
     * @return The default implementation returns the input parameter
     */ 
    protected RhinoCompletionRule[] initRules(RhinoCompletionRule[] rules, RhinoCompletor completor) {
        return rules;
    }
    
    protected void updateResultWindow(FileObject fo, String text) {
        Registry registry = TopComponent.getRegistry();
        RhinoEvalResultTopComponent resultWindow = null;
        for (Iterator it = registry.getOpened().iterator(); it.hasNext();) {
            Object object = it.next();
            if (object instanceof RhinoEvalResultTopComponent) {
                resultWindow = (RhinoEvalResultTopComponent) object;
            }
        }
        if(resultWindow != null) {
            String filename = "-";
            if (fo != null) {
               filename = fo.getNameExt();
            }
            resultWindow.setResult(filename, text);
        }
    }
    
}
