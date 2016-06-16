/**
 * Represent the table of namespaces
 */
package mcs.symtab;

import java.util.List;
import java.util.ArrayList;

public class NamespaceTable {
    private List<NamespaceInfo> content;

    public NamespaceTable() {
        this.content = new ArrayList<NamespaceInfo>();
    }

    public void add(NamespaceInfo ni) {
        this.content.add(ni);
    }

    public boolean exists(String name, NamespaceInfo base, List<NamespaceInfo> usedns) {
        for (NamespaceInfo ni : this.content) {
            if (ni.name().equals(name) && (ni.parent().equals(base) || usedns.contains(ni)))
                return true;
        }
        return false;
    }

    public NamespaceInfo lookup(String name, NamespaceInfo base, List<NamespaceInfo> usedns) {
        for (NamespaceInfo ni : this.content) {
            if (ni.name().equals(name) && (ni.parent().equals(base) || usedns.contains(ni)))
                return ni;
        }
        return null;
    }
}


