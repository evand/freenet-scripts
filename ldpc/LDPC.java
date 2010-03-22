import java.util.Random;

public class LDPC {
	private SparseMatrix orig;
	//private SparseMatrix encode;

	private final int n, k;

	public LDPC(int n, int k) {
		if (k <= 0 || n <= k) throw new IllegalArgumentException();
		this.n = n;
		this.k = k;
		orig = new SparseMatrix(n-k, n);
		//encode = new SparseMatrix(n-k, n);

	}

	public void fillConst(Random rand, int rowRank, int colRank) {
		if (rowRank < 2 || colRank < 2) throw new IllegalArgumentException();
		if (rowRank >= n / 2 || colRank >= (n-k) / 2) throw new IllegalArgumentException();
		if (rowRank * (n-k) != colRank * n) throw new IllegalArgumentException();
		int[] rRanks = new int[n-k];
		int[] cRanks = new int[n];
		for (int i = 0; i < rRanks.length; i++) {
			rRanks[i] = rowRank;
		}
		for (int i = 0; i < cRanks.length; i++) {
			cRanks[i] = colRank;
		}
		fill(rand, rRanks, cRanks);
	}

	private void fill(Random rand, int[] rowRanks, int[] colRanks) {
		if (rowRanks.length != n-k) throw new IllegalArgumentException();
		if (colRanks.length != n) throw new IllegalArgumentException();
		int totalRow = 0;
		int totalCol = 0;
		for (int i = 0; i < rowRanks.length; i++) {
			if (rowRanks[i] <= 0 || rowRanks[i] > n)
				throw new IllegalArgumentException();
			totalRow += rowRanks[i];
		}
		for (int i = 0; i < colRanks.length; i++) {
			if (colRanks[i] <= 0 || colRanks[i] > n-k)
				throw new IllegalArgumentException();
			totalCol += colRanks[i];
		}
		if (totalRow != totalCol) throw new IllegalArgumentException();

		int[] connections = new int[totalRow];
		int[] connRow = new int[totalRow];
		for (int i = 0, j = 0; i < n-k; i++) {
			for (int a = 0; a < rowRanks[i]; a++) connRow[j++] = i;
		}
		for (int i = 0, j = 0; i < n; i++) {
			for (int a = 0; a < colRanks[i]; a++) connections[j++] = i;
		}
		shuffle(rand, connections);
		boolean done = false;
		int nFixes = 0;
		//System.out.println("First fill attempt, checking...");
		while (!done) {
			done = true;
			for (int i = 0; i < totalRow; i++) {
				for (int j = i + 1; j < totalRow && connRow[i] == connRow[j]; j++) {
					if (connections[i] == connections[j]) {
						done = false;
						int s = rand.nextInt(totalRow);
						int t = connections[j];
						connections[j] = connections[s];
						connections[s] = t;
					}
				}
			}
			nFixes++;
			//System.out.println("Completed pass " + nFixes + ", done: " + done);
			assert nFixes <= 10;
		}
		//System.out.println("Assignment complete, populating matrix with " + totalRow + " connections.");
		for (int i = 0; i < totalRow; i++) {
			//if (((i-1) % 100) == 0) System.out.print(".");
			orig.set(connRow[i], connections[i], true);
		}
		//System.out.println();
		for (int i = 0; i < n-k; i++) {
			assert orig.rowRank(i) == rowRanks[i];
		}

		//encode = new SparseMatrix(orig);
		//System.out.println("Fill complete.");
	}

	public static void shuffle(Random rand, int[] a) {
		for (int i = 0; i < a.length - 1; i++) {
			int j = i + rand.nextInt(a.length - i);
			int t = a[i];
			a[i] = a[j];
			a[j] = t;
		}
	}

	public boolean check() {
		return orig.check();// && encode.check();
	}

	public boolean simulateDecode(boolean[] avail) {
		if (avail.length != n) throw new IllegalArgumentException();
		int nAvail = 0;
		for (int i = 0; i < avail.length; i++)
			if (avail[i]) nAvail++;
		if (nAvail == n) return true;

		boolean complete = false;
		boolean changed = true;
		while (changed && !complete) {
			complete = true;
			changed = false;
			for (int i = 0; i < n-k; i++) {
				SparseVector constraint = orig.rows[i];
				int nHave = 0;
				for (int j = 0; j < constraint.vals.length; j++) {
					if (constraint.vals[j] == -1) continue;
					if (avail[constraint.vals[j]]) nHave++;
				}
				if (nHave == constraint.getRank() - 1) {
					changed = true;
					for (int j = 0; j < constraint.vals.length; j++) {
						if (constraint.vals[j] == -1) continue;
						avail[constraint.vals[j]] = true;
					}
				} else if (nHave < constraint.getRank() - 1) {
					complete = false;
				}
			}
		}

		assert !complete || (nAvail >= k);
		return complete;
	}

	public static void main(String[] args) {
		int nData = 4096;
		int nCheck = nData;

		int nTrials = 100;
		int[] nNeeded = new int[nTrials];
		boolean[] avail = new boolean[nData + nCheck];
		int[] availOrder = new int[nData + nCheck];

		System.out.println("Simulating " + nTrials + " trials with "
				+ nData + " data blocks.");
		for (int i = 0; i < nTrials; i++) {
			System.out.print(".");
			Random rand = new Random(i);
			for (int j = 0; j < avail.length; j++) avail[j] = false;
			for (int j = 0; j < availOrder.length; j++) availOrder[j] = j;
			shuffle(rand, availOrder);
			LDPC code = new LDPC(nData + nCheck, nData);
			code.fillConst(rand, 6, 3);
			nNeeded[i] = -1;
			for (int j = 0; j < avail.length; j++) {
				if (avail[availOrder[j]]) continue;
				avail[availOrder[j]] = true;
				if (j < nData) continue;
				if (code.simulateDecode(avail)) {
					nNeeded[i] = j + 1;
					break;
				}
			}
			assert nNeeded[i] > 0;
			assert nNeeded[i] < nData + nCheck;
		}
		System.out.println();
		int min = nData + nCheck;
		int max = 0;
		int mean = 0;
		for (int i = 0; i < nTrials; i++) {
			if (nNeeded[i] < min) min = nNeeded[i];
			if (nNeeded[i] > max) max = nNeeded[i];
			mean += nNeeded[i];
		}
		mean /= nTrials;
		System.out.println("Sim complete.");
		System.out.println("Min needed:  " + min + "\t" + (((double)min)/(nData+nCheck)));
		System.out.println("Max needed:  " + max + "\t" + (((double)max)/(nData+nCheck)));
		System.out.println("Mean needed: " + mean + "\t" + (((double)mean)/(nData+nCheck)));
	}
}
