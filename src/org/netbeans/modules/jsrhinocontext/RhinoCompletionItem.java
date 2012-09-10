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

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import javax.swing.ImageIcon;
import javax.swing.JToolTip;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyledDocument;
import org.netbeans.api.editor.completion.Completion;
import org.netbeans.spi.editor.completion.CompletionItem;
import org.netbeans.spi.editor.completion.CompletionResultSet;
import org.netbeans.spi.editor.completion.CompletionTask;
import org.netbeans.spi.editor.completion.support.AsyncCompletionQuery;
import org.netbeans.spi.editor.completion.support.AsyncCompletionTask;
import org.netbeans.spi.editor.completion.support.CompletionUtilities;
import org.openide.util.Exceptions;
import org.openide.util.Utilities;

public class RhinoCompletionItem implements CompletionItem {

    private String text;
    private String preview;
    private String type;
    private String context;
    private static ImageIcon fieldIcon =
            new ImageIcon(Utilities.loadImage("org/netbeans/modules/jsrhinocontext/rhino.jpg"));
    private static Color fieldColor = Color.decode("0x0000B2");
    private int caretOffset;
    private int matchOffset;

    public RhinoCompletionItem(String text, int matchOffset, int caretOffset) {
        // Split fields
        String[] fields = text.split(RhinoCompletor.FIELD_SEPARATOR);
        this.text = fields[0];
        // Trim suggestion text
        this.preview = this.text + "  ";
        if (this.preview.length() > 60) {
            this.preview = this.preview.substring(0, 59) + "…  ";
        }
        this.type = fields[1];
        this.context = fields[2];
        this.matchOffset = matchOffset;
        this.caretOffset = caretOffset;
    }

    public void defaultAction(JTextComponent jTextComponent) {
        try {
            StyledDocument doc = (StyledDocument) jTextComponent.getDocument();
            doc.remove(matchOffset, caretOffset - matchOffset);
            doc.insertString(matchOffset, text, null);
            Completion.get().hideAll();
        } catch (BadLocationException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    public void processKeyEvent(KeyEvent arg0) {
    }

    public int getPreferredWidth(Graphics graphics, Font font) {
        return CompletionUtilities.getPreferredWidth(preview, type, graphics, font);
    }

    public void render(Graphics g, Font defaultFont, Color defaultColor,
            Color backgroundColor, int width, int height, boolean selected) {
        CompletionUtilities.renderHtml(fieldIcon, preview, type, g, defaultFont,
                (selected ? Color.white : fieldColor), width, height, selected);
    }

    public CompletionTask createDocumentationTask() {
        return new AsyncCompletionTask(new AsyncCompletionQuery() {
            protected void query(CompletionResultSet completionResultSet, Document document, int offset) {
                completionResultSet.setDocumentation(new RhinoCompletionDocumentation(RhinoCompletionItem.this));
                completionResultSet.finish();
            }
        });
    }

    public CompletionTask createToolTipTask() {
        return new AsyncCompletionTask(new AsyncCompletionQuery() {

            protected void query(CompletionResultSet completionResultSet, Document document, int i) {
                JToolTip toolTip = new JToolTip();
                toolTip.setTipText("Press Enter to insert \"" + text + "\"");
                completionResultSet.setToolTip(toolTip);
                completionResultSet.finish();
            }
        });
    }

    public boolean instantSubstitution(JTextComponent arg0) {
        return false;
    }

    public int getSortPriority() {
        return 0;
    }

    public CharSequence getSortText() {
        return text;
    }

    public String getText() {
        return text;
    }

    public String getType() {
        return type;
    }

    public String getContext() {
        return context;
    }
    
    public CharSequence getInsertPrefix() {
        return text;
    }

}
