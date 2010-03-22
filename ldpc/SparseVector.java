public class SparseVector {
	public final int length;
	private int rank;

	// sorted list of true positions; -1 is empty
	public int[] vals;

	public SparseVector(int length) {
		if (length <= 0) throw new ArrayIndexOutOfBoundsException();
		this.length = length;
		vals = new int[6 < length ? 6 : length];
		for (int i = 0; i < vals.length; i++) vals[i] = -1;
		rank = 0;
	}

	public SparseVector(SparseVector v) {
		this.length = v.length;
		this.rank = v.rank;
		vals = new int[v.vals.length];
		for (int i = 0; i < vals.length; i++) vals[i] = v.vals[i];
	}

	public void set(int i, boolean val) {
		if (i < 0 || i >= length) throw new ArrayIndexOutOfBoundsException(i);
		assert check();
		int j;
		for (j = 0; j < vals.length; j++) {
			if (val && vals[j] == i) return; //already set
			if (val && vals[j] > i) break; //need to set
			if (!val && vals[j] > i) return; //already cleared
			if (!val && vals[j] == i) {
				vals[j] = -1;
				rank--;
				assert check();
				return;
			}
		}
		if (!val) {
			//already cleared
			assert j == vals.length;
			assert vals[vals.length - 1] < i;
			return;
		}
		//need to set
		if (rank == vals.length) {
			expand();
		} else if (j == vals.length) {
			compact();
			vals[rank++] = i;
			assert check();
			return;
		}
		assert vals[j] == -1 || vals[j] > i;
		while (j < vals.length && vals[j] != -1) {
			int t = vals[j];
			vals[j] = i;
			i = t;
			j++;
		}
		if (j < vals.length) {
			assert vals[j] == -1;
			vals[j] = i;
			rank++;
			assert check();
			return;
		}
		compact();
		assert vals[rank] == -1;
		vals[rank] = i;
		assert check();
		return;
	}

	public boolean get(int i) {
		if (i < 0 || i >= length) throw new ArrayIndexOutOfBoundsException(i);
		for (int j = 0; j < vals.length; j++) {
			if (vals[j] == i) return true;
			if (vals[j] > i) return false;
		}
		return false;
	}

	public int getRank() {
		return rank;
	}

	private void expand() {
		if (vals.length == length) throw new IllegalArgumentException();
		assert check();
		int newsize = 2 * vals.length;
		if (newsize > length) newsize = length;
		int[] newvals = new int[newsize];
		int i, j;
		for (i = 0, j = 0; i < rank && j < vals.length; i++, j++) {
			while (j < vals.length && vals[j] == -1) j++;
			newvals[i] = vals[j];
		}
		for (; i < newvals.length; i++) newvals[i] = -1;
		vals = newvals;
		assert check();
	}

	private void compact() {
		int i, j;
		assert check();
		for (i = 0, j = 0; i < rank && j < vals.length; i++, j++) {
			while (j < vals.length && vals[j] == -1) j++;
			vals[i] = vals[j];
		}
		for (; i < vals.length; i++) vals[i] = -1;
		assert check();
	}

	public boolean check() {
		if (rank < 0 || rank > length) return false;
		if (vals.length > length) return false;
		int p = -1;
		int r = 0;
		for (int i = 0; i < vals.length; i++) {
			if (vals[i] == -1) continue;
			r++;
			if (vals[i] <= p) return false;
			p = vals[i];
			if (p >= length) return false;
		}
		return (r == rank);
	}

	/* Add v to this.  Note: on booleans, addition == xor. */
	public void add(SparseVector v) {
		if (length != v.length) throw new IllegalArgumentException();
		for (int i = 0; i < v.vals.length; i++) {
			if (v.vals[i] == -1) continue;
			set(v.vals[i], !get(v.vals[i]));
		}
	}

	/* Multiple v by this.  Note: on booleans, multiplication == and. */
	public void mult(SparseVector v) {
		if (length != v.length) throw new IllegalArgumentException();
		for (int i = 0; i < vals.length; i++) {
			if (vals[i] == -1) continue;
			if (!v.get(vals[i])) {
				vals[i] = -1;
				rank--;
			}
		}
		assert check();
	}

	public boolean dotProd(SparseVector v) {
		if (length != v.length) throw new IllegalArgumentException();
		boolean d = false;
		for (int i = 0; i < vals.length; i++) {
			if (vals[i] == -1) continue;
			if (v.get(vals[i])) d = !d;
		}
		return d;
	}

	public boolean dotProd(boolean[] v) {
		if (length != v.length) throw new IllegalArgumentException();
		boolean d = false;
		for (int i = 0; i < vals.length; i++) {
			if (vals[i] == -1) continue;
			if (v[i]) d = !d;
		}
		return d;
	}
}
