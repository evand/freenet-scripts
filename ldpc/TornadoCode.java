import java.util.Random;

public class TornadoCode extends LDPC {

	private static final boolean verbose = false;

	public TornadoCode(Random rand, int n, int k, int d) {
		super(n, k);

		int[] rowRanks = new int[n-k];
		int[] colRanks = new int[n];

		int[] lArray = makeLDegreeArray(d, n);
		int nEdges = 0;
		for (int i = 0; i < lArray.length; i++) nEdges += i * lArray[i];
		int[] rArray = makeRDegreeArray(n-k, nEdges);
		int i, j;
		for (i = 0, j = 0; i < lArray.length; i++) {
			for (int l = 0; l < lArray[i]; l++) {
				colRanks[j++] = i;
			}
		}
		for (i = 0, j = 0; i < rArray.length; i++) {
			for (int r = 0; r < rArray[i]; r++) {
				rowRanks[j++] = i;
			}
		}
		shuffle(rand, rowRanks);
		shuffle(rand, colRanks);
		fill(rand, rowRanks, colRanks);
	}

	public static int[] makeLDegreeArray(int d, int nBlocks) {
		if (d < 2) throw new IllegalArgumentException();
		if (nBlocks < 3) throw new IllegalArgumentException();
		if (nBlocks < d + 2) throw new IllegalArgumentException();

		int[] lArray = new int[d+2];
		
		double h_d = 0;
		for (int i = 1; i <=d; i++) h_d += 1.0 / ((double)(i));

		int nTotal = 0;
		double rem = 0;
		lArray[0] = 0;
		lArray[1] = 0;
		if (verbose) System.out.println("0\t0\t0\n1\t0\t0");
		double avgLeftDegree = h_d * (d + 1) / ((double)(d));
		int nEdges = (int)(((double)(nBlocks)) * avgLeftDegree + 0.5);

		for (int i = 2; i <= d + 1; i++) {
			double lambda_i = 1.0 / (h_d * ((double)(i - 1)));
			//var naming to match normal conventions:
			//l_i = number of edges of left degree i
			//L_i = number of left nodes of degree i
			double L_i = nEdges * lambda_i / ((double)(i));
			lArray[i] = (int)(L_i + rem + 0.5);
			//if (lArray[i] < 1) lArray[i] = 1;
			if (lArray[i] + nTotal > nBlocks) lArray[i] = nBlocks - nTotal;
			nTotal += lArray[i];
			if (verbose) System.out.println(i + "\t" + lArray[i] + "\t" + L_i);
			if (nTotal == nBlocks) break;
			rem += L_i - ((double)(lArray[i]));
			assert nTotal < nBlocks;
			assert rem >= -0.5 && rem < 0.5 : "\nrem: " + rem;
			assert (i == 2) || (lArray[i] <= lArray[i-1] + 1);
		}
		assert nTotal == nBlocks :
			"\nnBlocks: " + nBlocks + "\nnTotal: " + nTotal + "\nrem: " + rem;
		return lArray;
	}

	public static int[] makeRDegreeArray(int nCheckBlocks, int nEdges) {
		//nCheckBlocks = number of right nodes
		if (nCheckBlocks < 1) throw new IllegalArgumentException();
		if (nEdges < nCheckBlocks * 3) throw new IllegalArgumentException();

		int[] rArray;
		double avgRightDegree = ((double)(nEdges)) / nCheckBlocks;
		double alpha = computeAlpha(avgRightDegree);
		int maxDegree;
		for (maxDegree = (int)avgRightDegree;; maxDegree++) {
			double r_i = rho(alpha, maxDegree);
			double r_i_1 = rho(alpha, maxDegree - 1);
			if (r_i_1 < r_i) continue;
			if (nEdges * r_i < 0.000001) break;
		}
		assert maxDegree >= 3;

		rArray = new int[maxDegree + 1];
		rArray[0] = 0;
		rArray[1] = 0;
		rArray[2] = 0;
		
		if (verbose) {
			System.out.println("Computing right degrees, nCB=" + nCheckBlocks +
					", nE=" + nEdges);
			System.out.println("Presumed max degree: " + maxDegree);
			System.out.println("Alpha: " + alpha);
		}

		int nTotalEdges = 0;
		int nTotalNodes = 0;
		/*
		double rem = ((double)(nEdges)) * rho(alpha, 1);
		System.out.println("0\t0\t0");
		System.out.println("\n1\t0\t" + rem);
		rem += ((double)(nEdges)) * rho(alpha, 2);
		System.out.println("2\t0\t" + rem);
		*/
		double rem = 0;
		
		int i;
		for (i = maxDegree; i >= 3; i--) {
			assert nTotalEdges < nEdges;
			assert nTotalNodes < nCheckBlocks;
			assert i * (nCheckBlocks - nTotalNodes) >= (nEdges - nTotalEdges);

			double rho_i = rho(alpha, i);
			double r_i = ((double)(nEdges)) * rho_i;
			rArray[i] = (int)((r_i + rem) / ((double)(i)) + 0.5);
			if (rArray[i] * i > nEdges - nTotalEdges) rArray[i]--;
			rem += r_i - ((double)(rArray[i] * i));
			nTotalNodes += rArray[i];
			nTotalEdges += rArray[i] * i;
			if (verbose && nTotalNodes > 0) {
				System.out.println(i + "\t" + rArray[i] + "\t" + rho_i +
						"\t" + r_i + "\t" + rem);
			}
			if (nTotalNodes >= nCheckBlocks) break;
			if (nTotalEdges >= nEdges) break;
		}
		if (nTotalNodes < nCheckBlocks || nTotalEdges < nEdges) {
			assert nTotalNodes < nCheckBlocks && nTotalEdges < nEdges;
			//assert nEdges - nTotalEdges >= 3 * (nCheckBlocks - nTotalNodes);
			assert nEdges - nTotalEdges <= 4 * (nCheckBlocks - nTotalNodes);
			int p4 = (nEdges - nTotalEdges) - 3 * (nCheckBlocks - nTotalNodes);
			int p3 = nCheckBlocks - nTotalNodes - p4;
			assert nTotalEdges + 3 * p3 + 4 * p4 == nEdges;
			rArray[4] += p4;
			rArray[3] += p3;
			nTotalNodes += p3 + p4;
			nTotalEdges += 3 * p3 + 4 * p4;
			assert rArray[4] > 0;
			assert rArray[3] > 0;
		}

		if (verbose) {
			System.out.println("nCheckBlocks: " + nCheckBlocks);
			System.out.println("nTotalNodes: " + nTotalNodes);
			System.out.println("nEdges: " + nEdges);
			System.out.println("nTotalEdges: " + nTotalEdges);
		}
		assert nTotalNodes == nCheckBlocks;
		assert nTotalEdges == nEdges;
		return rArray;
	}

	//compute a s.t. (a*exp(a))/(exp(a)-1) = ar
	public static double computeAlpha(double ar) {
		if (ar <= 1.0) throw new IllegalArgumentException();
		//precision problems if ar too large
		if (ar > 30) throw new IllegalArgumentException();

		//0 < ar => ar < a < ar + 1
		double amin = ar;
		double amax = ar + 1.0;
		double amid = (amin + amax) / 2.0;
		while (amid > amin && amid < amax) {
			double arr = amid * (Math.exp(amid) / (Math.exp(amid) - 1.0));
			if (arr < ar ) {
				amin = amid;
			} else {
				amax = amid;
			}
			amid = (amin + amax) / 2.0;
		}
		return amid;
	}

	public static double rho(double a, int i) {
		return (Math.pow(a, i-1)) / (Math.exp(a) * fact(i-1));
	}

	public static double fact(int i) {
		if (i < 0 || i > 170) throw new IllegalArgumentException();
		double f = 1.0;
		for (int j = 1; j <= i; j++) f *= ((double)(j));
		return f;
	}

	public static void main(String[] args) {
		int nBlocks = 20480;
		int nCheck = 10240;
		int d = 100;
		System.out.println("Degree distribution for n=" + nBlocks + ", d=" + d);
		int[] lArray = makeLDegreeArray(d, nBlocks);
		/*
		for (int i = 0; i < lArray.length; i++) {
			System.out.println(i + "\t" + lArray[i]);
		}
		*/

		int nEdges = 0;
		for (int i = 0; i < lArray.length; i++) nEdges += i * lArray[i];
		System.out.println("\nnEdges: " + nEdges);
		int[] rArray = makeRDegreeArray(nCheck, nEdges);
	}
}
