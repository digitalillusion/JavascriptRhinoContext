/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.modules.jsrhinocontext.ui;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.Iterator;
import org.netbeans.api.editor.mimelookup.MimeLookup;
import org.netbeans.api.editor.mimelookup.MimePath;
import org.netbeans.modules.jsrhinocontext.RhinoCompletionProvider;
import org.netbeans.spi.editor.completion.CompletionProvider;
import org.openide.cookies.EditorCookie;

import org.openide.awt.ActionRegistration;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionID;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;

@ActionID(category = "Tools",
id = "org.netbeans.modules.jsrhinocontext.RhinoCompletionEnableAction")
@ActionRegistration(iconBase = "org/netbeans/modules/jsrhinocontext/rhino.jpg",
displayName = "#CTL_RhinoCompletionEnableAction")
@ActionReferences({
    @ActionReference(path = "Menu/Tools", position = 140),
    @ActionReference(path = "Editors/text/javascript/Popup", position = 400)
})
@Messages("CTL_RhinoCompletionEnableAction=Toggle Rhino content assist")
public final class RhinoCompletionEnableAction implements ActionListener {

    private final EditorCookie context;

    public RhinoCompletionEnableAction(EditorCookie context) {
        this.context = context;
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
       Lookup lookup = MimeLookup.getLookup(MimePath.get("text/javascript"));
       Collection<? extends CompletionProvider> col = lookup.lookupAll(CompletionProvider.class);
        for (Iterator<? extends CompletionProvider> it = col.iterator(); it
                .hasNext();) {
            CompletionProvider completionProvider = it.next();
            if (completionProvider instanceof  RhinoCompletionProvider) {
                RhinoCompletionProvider rcp = (RhinoCompletionProvider) completionProvider;
                rcp.toggleEnable();
            }
        }
    }
}
