public class SparseMatrix {
	public final int rr, cc;
	public final SparseVector[] rows;
	private int[] rowRanks;
	private int[] colRanks;

	public SparseMatrix(int r, int c) {
		rr = r;
		cc = c;
		rows = new SparseVector[r];
		for (int i = 0; i < r; i++) {
			rows[i] = new SparseVector(c);
		}
		rowRanks = new int[rr];
		colRanks = new int[cc];
	}

	//elementary row ops:
	public void swapRows(int r1, int r2) {
		if (r1 == r2) throw new IllegalArgumentException();
		SparseVector t = rows[r1];
		rows[r1] = rows[r2];
		rows[r2] = t;
		int tmp = rowRanks[r1];
		rowRanks[r2] = rowRanks[r1];
		rowRanks[r1] = tmp;
	}

	//rows[d] += rows[s]
	public void addRow(int s, int d) {
		if (s == d) throw new IllegalArgumentException();
		rows[d].add(rows[s]);
	}

	//column swapping is sometimes allowed...
	//O(rr)
	public void swapCols(int c1, int c2) {
		if (c1 == c2) throw new IllegalArgumentException();
		for (int i = 0; i < rr; i++) {
			boolean a = rows[i].get(c1);
			boolean b = rows[i].get(c2);
			if (a != b) {
				rows[i].set(c1, b);
				rows[i].set(c2, a);
			}
		}
	}

	public boolean check() {
		for (int i = 0; i < rr; i++) {
			if (!rows[i].check()) return false;
		}
		return true;
	}

	public int rowRank(int r) {
		return rowRanks[r];
	}

	public int colRank(int c) {
		return colRanks[c];
	}

	public boolean get(int r, int c) {
		return rows[r].get(c);
	}

	public void set(int r, int c, boolean val) {
		boolean old = get(r, c);
		if (old == val) return;
		if (old) {
			rowRanks[r]--;
			colRanks[c]--;
		} else {
			rowRanks[r]++;
			colRanks[c]++;
		}
		rows[r].set(c, val);
	}
}
