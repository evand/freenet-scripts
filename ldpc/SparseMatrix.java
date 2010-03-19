import java.util.Random;

class SparseMatrix {
	int rows, cols;
	boolean[][] origMat;
	boolean[][] encodeMat;

	int[][] constraints;

	public SparseMatrix(int rows, int cols) {
		if (rows <= 0 || cols <= 0 || rows >= cols) throw new IllegalArgumentException();

		this.rows = rows;
		this.cols = cols;

		origMat = new boolean[rows][cols];
		encodeMat = new boolean[rows][cols];
	}

	public boolean fillFlat(Random rand, int perRow, int perCol) {
		if (perRow <= 0 || perCol <= 0) throw new IllegalArgumentException();
		if (cols * perCol != rows * perRow) throw new IllegalArgumentException();

		int[] pr = new int[rows];
		int[] pc = new int[cols];

		for (int i = 0; i < rows; i++) pr[i] = perRow;
		for (int i = 0; i < cols; i++) pc[i] = perCol;

		return fillMatrix(rand, pr, pc);
	}

	public boolean fillMatrix(Random rand, int[] perRow, int[] perCol) {
		if (perRow.length != rows || perCol.length != cols)
			throw new IllegalArgumentException();
		int rs = 0;
		int cs = 0;
		for (int i = 0; i < rows; i++) {
			if (perRow[i] <= 0) throw new IllegalArgumentException();
			rs += perRow[i];
		}
		for (int i = 0; i < cols; i++) {
			if (perCol[i] <= 0) throw new IllegalArgumentException();
			cs += perCol[i];
		}
		if (cs != rs) throw new IllegalArgumentException();

		for (int i = 0; i < rows; i++) {
			int assigned = 0;
			while (assigned < perRow[i]) {
				int j = rand.nextInt(cols);
				if (origMat[i][j]) continue;
			}
		}

		try {
			makeReducedRowEchelon();
		} catch (SingularMatrixException e) {
			return false;
		}
		return true;
	}

	/** Knuth shuffle */
	public static void shuffle(Random rand, int[] a) {
		shuffle(rand, a, 0, a.length);
	}

	/** Knuth shuffle */
	public static void shuffle(Random rand, int[] a, int first, int last) {
		for (int i = 0; i < a.length - 1; i++) {
			int j = i + rand.nextInt(a.length - i);
			int t = a[i];
			a[i] = a[j];
			a[j] = t;
		}
	}

	private void makeRowEchelon() throws SingularMatrixException {
		for (int i = 0; i < rows; i++) {
			//first: swap with a row to get the leading 1 in the correct col
			int t;
			for (t = i; t < rows; t++) if (encodeMat[t][i]) break;
			if (t == rows) throw new SingularMatrixException();
			boolean[] tmp = encodeMat[t];
			encodeMat[t] = encodeMat[i];
			encodeMat[i] = tmp;

			//second: add into lower rows so that only 0s below the leading 1.
			for (int j = i + 1; j < rows; j++) {
				if (encodeMat[j][i] == false) continue;
				for (int k = i; k < cols; k++) encodeMat[j][k] ^= encodeMat[i][k];
			}

			//sanity check
			for (int j = 0; j < i; j++) assert encodeMat[i][j] == false;
		}
	}

	private void makeReducedRowEchelon() throws SingularMatrixException {
		makeRowEchelon();
		for (int i = rows - 1; i >= 0; i--) {
			for (int j = i - 1; j >= 0; j--) {
				if (encodeMat[j][i] == false) continue;
				for (int k = i; k < cols; k++) encodeMat[j][k] ^= encodeMat[i][k];
			}
		}
		assert isRREF();
	}

	private boolean isRREF() {
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < rows; j++) {
				if ((i == j) != (encodeMat[i][j])) return false;
			}
		}
		return true;
	}

	public boolean checkConstraints(boolean[] encodedData) {
		if (encodedData.length != cols) throw new IllegalArgumentException();
		for (int i = 0; i < rows; i++) {
			boolean check = false;
			for (int j = 0; j < constraints[i].length; j++) {
				check ^= encodedData[constraints[i][j]];
			}
			if (check) return false;
		}
		return true;
	}

	public boolean decode(boolean[] encodedData, boolean[] available) {
		if (encodedData.length != cols) throw new IllegalArgumentException();
		if (available.length != cols) throw new IllegalArgumentException();

		for (int i = 0; i < cols; i++) {
			if (!available[i] && encodedData[i]) throw new IllegalArgumentException();
		}

		boolean progress = true;
		while (progress) {
			progress = false;
			for (int i = 0; i < rows; i++) {
				//attempt to decode from the ith constraint
				int av = 0;
				for (int j = 0; j < constraints[i].length; j++) {
					if (available[constraints[i][j]]) av++;
				}
				if (av != constraints[i].length - 1) continue;
				progress = true;
				boolean d = false;
				for (int j = 0; j < constraints[i].length; j++) {
					d ^= encodedData[constraints[i][j]];
				}
				for (int j = 0; j < constraints[i].length; j++) {
					if (!available[constraints[i][j]]) {
						encodedData[constraints[i][j]] = d;
						available[constraints[i][j]] = true;
					}
				}
			}
		}

		for (int i = 0; i < cols; i++) if (!available[i]) return false;
		return true;
	}

	//Check blocks come first, all check blocks are computed assuming all data blocks present.
	public void encode(boolean[] data) {
		if (data.length != cols) throw new IllegalArgumentException();
		for (int i = 0; i < rows; i++) if (data[i]) throw new IllegalArgumentException();
		
		for (int i = 0; i < rows; i++) {
			boolean c = false;
			for (int j = rows; j < cols; j++) {
				c ^= encodeMat[i][j] && data[j];
			}
			data[i] = c;
		}

		assert checkConstraints(data);
	}

	public static SparseMatrix makeLDPCMatrix(Random rand, int nData) {
		if (nData <= 10) throw new IllegalArgumentException();//FIXME: actual limit?

		SparseMatrix m = null;

		int maxTries = 1;
		boolean success = false;
		for (int t = 0; t < maxTries; t++) {
			m = new SparseMatrix(nData, 2 * nData);
			success = m.fillFlat(rand, 6, 3);
			if (success) break;
		}
		return m;//null iff making above failed after maxTries attempts.
	}

	public static void main(String[] args) {
		int nMats = 1;
		int nSimsPerMat = 1;
		int nData = 5000;
		double pAvail = 0.55;
		int nSuccess = 0;

		boolean[] odata = new boolean[2 * nData];
		boolean[] data = new boolean[2 * nData];
		boolean[] avail = new boolean[2 * nData];

		for (int i = 0; i < nMats; i++) {
			Random rand = new Random(i);
			SparseMatrix m = makeLDPCMatrix(rand, nData);
			for (int sim = 0; sim < nSimsPerMat; sim++) {
				for (int j = 0; j < 2 * nData; j++) {
					data[j] = (j >= nData) && rand.nextBoolean();
					odata[j] = data[j];
					avail[j] = rand.nextDouble() < pAvail;
				}

				m.encode(data);

				for (int j = 0; j < 2 * nData; j++) {
					if (!avail[j]) data[j] = false;
				}

				boolean success = m.decode(data, avail);
				if (success) {
					nSuccess++;
					for (int j = 0; j < 2 * nData; j++) {
						assert odata[j] == data[j];
					}
				}
			}
		}

		System.out.println("Results of " + nSimsPerMat + " sims on each of " + nMats + " codes, n=" + nData + ", p=" + pAvail + ":");
		System.out.println("Decodes: " + nSuccess);
	}
}

class SingularMatrixException extends Exception {
}
