import java.util.TreeSet;
import java.util.Iterator;

/** A sparse boolean vector
 */

public class SparseVector {
	public final int length;
	private final TreeSet<Integer> vals;

	public SparseVector(int length) {
		if (length <= 0) throw new ArrayIndexOutOfBoundsException();
		this.length = length;
		vals = new TreeSet<Integer>();
	}

	public void set(int i, boolean val) {
		if (i < 0 || i >= length) throw new ArrayIndexOutOfBoundsException(i);

		if (val) {
			vals.add(i);
		} else {
			vals.remove(i);
		}
	}

	public boolean get(int i) {
		return vals.contains(i);
	}

	public int getRank() {
		return vals.size();
	}

	public Iterator<Integer> iterator() {
		return vals.iterator();
	}

	/* Add v to this.  Note: on booleans, addition == xor. */
	public void add(SparseVector v) {
		for (Iterator<Integer> it = v.vals.iterator(); it.hasNext();) {
			int i = it.next();
			set(i, !get(i));
		}
	}

	/* Multiple v by this.  Note: on booleans, multiplication == and. */
	public void mult(SparseVector v) {
		for (Iterator<Integer> it = vals.iterator(); it.hasNext();) {
			int i = it.next();
			set(i, v.get(i));
		}
	}

	public boolean dotProd(SparseVector v) {
		boolean p = false;
		for (Iterator<Integer> it = vals.iterator(); it.hasNext();) {
			int i = it.next();
			if (v.get(i)) p = !p;
		}
		return p;
	}

	public boolean dotProd(boolean[] v) {
		boolean p = false;
		for (Iterator<Integer> it = vals.iterator(); it.hasNext();) {
			int i = it.next();
			if (v[i]) p = !p;
		}
		return p;
	}
}

