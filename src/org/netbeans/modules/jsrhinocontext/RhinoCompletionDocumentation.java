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

import java.net.URL;
import javax.swing.Action;
import org.netbeans.spi.editor.completion.CompletionDocumentation;

public class RhinoCompletionDocumentation implements CompletionDocumentation {
    
    private RhinoCompletionItem item;

    public RhinoCompletionDocumentation(RhinoCompletionItem item) {
        this.item = item;
    }

    
    @Override
    public String getText() {
        return "<strong>" + item.getType() + "<strong/><hr/>" +
               "<div>" + item.getContext() + "<div/>" + 
               "<p>Completion proposal: <pre>" + item.getText() + "</pre></p>";
    }

    @Override
    public URL getURL() {
        return null;
    }

    @Override
    public CompletionDocumentation resolveLink(String string) {
        return null;
    }

    @Override
    public Action getGotoSourceAction() {
        return null;
    }
    
}
