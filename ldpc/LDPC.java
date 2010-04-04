import java.util.Iterator;
import java.util.Random;
import java.util.Arrays;

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

	public void fill(Random rand, int[] rowRanks, int[] colRanks) {
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
		return simulateDecode(avail, true);
	}

	public boolean simulateDecode(boolean[] avail, boolean requireAll) {
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
				for (Iterator<Integer> it = constraint.iterator(); it.hasNext();) {
					if (avail[it.next()]) nHave++;
				}
				if (nHave == constraint.getRank() - 1) {
					changed = true;
					for (Iterator<Integer> it = constraint.iterator();
							it.hasNext();) {
						avail[it.next()] = true;
					}
				} else if (nHave < constraint.getRank() - 1) {
					complete = false;
				}
			}
		}

		assert !complete || (nAvail >= k);
		if (requireAll) return complete;

		//!requireAll: only worry about the data blocks
		for (int i = n-k; i < n; i++) if (!avail[i]) return false;
		return true;
	}

	public static void main(String[] args) {
		int nData = 20480;
		int nCheck = nData;
		int d = 200;

		int nTrials = 100;
		int[] nNeeded = new int[nTrials];
		int[] nUsed = new int[nTrials];
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
			/*
			LDPC code = new LDPC(nData + nCheck, nData);
			code.fillConst(rand, 6, 3);
			*/

			LDPC code = new TornadoCode(rand, nData + nCheck, nData, d);

			nNeeded[i] = -1;
			nUsed[i] = 0;
			for (int j = 0; j < avail.length; j++) {
				if (avail[availOrder[j]]) {
					continue;
				} else {
					nUsed[i]++;
				}
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
		System.out.println("Sim complete.\n");

		Arrays.sort(nNeeded);
		Arrays.sort(nUsed);

		int min = nNeeded[0];
		int max = nNeeded[nNeeded.length - 1];
		int median = nNeeded[nNeeded.length / 2];
		double mean = mean(nNeeded);

		System.out.println("Min needed:\t" + min + "\t" + (((double)min)/(nData+nCheck)));
		System.out.println("Max needed:\t" + max + "\t" + (((double)max)/(nData+nCheck)));
		System.out.println("Median needed:\t" + median + "\t" + (((double)median)/(nData+nCheck)));
		System.out.println("Mean needed:\t" + mean + "\t" + (((double)mean)/(nData+nCheck)));
		System.out.println();
		
		min = nUsed[0];
		max = nUsed[nUsed.length - 1];
		median = nUsed[nUsed.length / 2];
		mean = mean(nUsed);

		System.out.println("Min used:\t" + min + "\t" + (((double)min)/(nData+nCheck)));
		System.out.println("Max used:\t" + max + "\t" + (((double)max)/(nData+nCheck)));
		System.out.println("Median used:\t" + median + "\t" + (((double)median)/(nData+nCheck)));
		System.out.println("Mean used:\t" + mean + "\t" + (((double)mean)/(nData+nCheck)));
	}

	public static double mean(int[] a) {
		double m = 0;
		for (int i = 0; i < a.length; i++) m += a[i];
		m /= (double)(a.length);
		return m;
	}
}
