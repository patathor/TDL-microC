/**
 * MethodInfo -- class representing a method (ie : a function of a class)
 *
 * @author G. Dupont
 * @version 0.1
 */
package mcs.obj;

import mcs.gc.Register;
import mcs.symtab.*;

public class MethodInfo extends FunctionInfo {
	private Klass parent;
  private Klass.AccessSpecifier accSpec;
  private VirtualTable vtable;

	public MethodInfo(String name, Klass.AccessSpecifier as, Type ret, Klass parent, Register fr) {
		super(name, ret, null, fr);
		this.parent = parent;
    this.accSpec = as;
	}

	public MethodInfo(Klass.AccessSpecifier as, Klass parent, FunctionInfo other) {
		super(other.name(), other.returnType(), other.parameters(), null, other.register());
		this.parent = parent;
		this.accSpec = as;
	}

  public Klass parent() {
    return this.parent;
  }

  public Klass.AccessSpecifier accessSpecifier() {
    return this.accSpec;
  }

  // Vtable related
  public void assignVtable(VirtualTable t) {
    this.vtable = t;
  }

  public VirtualTable vtable() {
    return this.vtable;
  }

  @Override
  public String label() {
      return
          "_" + this.parent.completeName() + "." + this.name() + "__" + this.returnType() + this.makeParamsLabel();
  }

  public String shortLabel() {
      return
          "_" + this.name() + "__" + this.returnType() + this.makeParamsLabel();
  }

  @Override
  public boolean equals(FunctionInfo other) {
    if (other instanceof MethodInfo) {
      return this.equals((MethodInfo)other);
    }
    return false;
  }

  public boolean equals(MethodInfo other) {
    return super.equals(other) && other.parent().isEqualTo(this.parent);
  }

	@Override
	public String toString() {
		return this.returnType() + " " + this.parent.name() + "." + this.name() + makeParamsString();
	}
}


