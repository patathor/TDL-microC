/**
 * Class representing a constructor of the class
 *
 * @author G. Dupont
 * @version 0.1
 */
package mcs.obj;

import mcs.gc.Register;
import mcs.symtab.FunctionInfo;
import mcs.symtab.VoidType;

public class ConstructorInfo extends MethodInfo {
  public ConstructorInfo(Klass.AccessSpecifier as, Klass parent, Register fr) {
    super("__cstr__", as, new VoidType(), parent, fr);
    super.assignVtable(null);
  }

  @Override
  public void assignVtable(VirtualTable t) {
  }

  @Override
  public boolean equals(FunctionInfo other) {
    if (other instanceof ConstructorInfo)
      return this.equals((ConstructorInfo)other);
    return false;
  }

  public boolean equals(ConstructorInfo other) {
    return this.parent().isEqualTo(other.parent()) && this.similar(other);
  }

  @Override
  public String label() {
      return this.parent().completeName() + "__cstr__" + this.makeParamsLabel();
  }

	@Override
	public String toString() {
		return this.parent().name() + "." + this.parent().name() + makeParamsString();
	}
}


