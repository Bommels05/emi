package net.minecraft.client.search;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import dev.emi.emi.runtime.EmiLog;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.apache.logging.log4j.Logger;

/**
 * Provides an efficient way to search for a text in multiple texts.
 */
@Environment(EnvType.CLIENT)
public class SuffixArray<T> {
	private static final boolean PRINT_COMPARISONS = Boolean.parseBoolean(System.getProperty("SuffixArray.printComparisons", "false"));
	private static final boolean PRINT_ARRAY = Boolean.parseBoolean(System.getProperty("SuffixArray.printArray", "false"));
	private static final Logger LOGGER = EmiLog.LOG;
	private static final int END_OF_TEXT_MARKER = -1;
	private static final int END_OF_DATA = -2;
	protected final List<T> objects = Lists.<T>newArrayList();
	private final List<Integer> characters = new ArrayList<>();
	private final List<Integer> textStarts = new ArrayList<>();
	private List<Integer> suffixIndexToObjectIndex = new ArrayList<>();
	private List<Integer> offsetInText = new ArrayList<>();
	private int maxTextLength;

	/**
	 * Adds a text with the corresponding object.
	 * 
	 * <p>You are not allowed to call this method after calling {@link #build()} method.
	 * 
	 * <p>Takes O({@code text.length()}) time.
	 */
	public void add(T object, String text) {
		this.maxTextLength = Math.max(this.maxTextLength, text.length());
		int i = this.objects.size();
		this.objects.add(object);
		this.textStarts.add(this.characters.size());

		for(int j = 0; j < text.length(); ++j) {
			this.suffixIndexToObjectIndex.add(i);
			this.offsetInText.add(j);
			this.characters.add((int)text.charAt(j));
		}

		this.suffixIndexToObjectIndex.add(i);
		this.offsetInText.add(text.length());
		this.characters.add(-1);
	}

	/**
	 * Builds a suffix array with added texts.
	 * 
	 * <p>You are not allowed to call this method multiple times.
	 * 
	 * <p>Takes O(N * log N * log M) time on average where N is the sum of all text
	 * length added, and M is the maximum text length added.
	 */
	public void build() {
		int i = this.characters.size();
		int[] is = new int[i];
		int[] js = new int[i];
		int[] ks = new int[i];
		int[] ls = new int[i];
		Comparator<Integer> intComparator = (ix, jx) -> js[ix] == js[jx] ? Integer.compare(ks[ix], ks[jx]) : Integer.compare(js[ix], js[jx]);
		BiConsumer<Integer, Integer> swapper = (ix, jx) -> {
			if (!Objects.equals(ix, jx)) {
				int kx = js[ix];
				js[ix] = js[jx];
				js[jx] = kx;
				kx = ks[ix];
				ks[ix] = ks[jx];
				ks[jx] = kx;
				kx = ls[ix];
				ls[ix] = ls[jx];
				ls[jx] = kx;
			}

		};

		for(int j = 0; j < i; ++j) {
			is[j] = this.characters.get(j);
		}

		int j = 1;

		for(int k = Math.min(i, this.maxTextLength); j * 2 < k; j *= 2) {
			for(int l = 0; l < i; ls[l] = l++) {
				js[l] = is[l];
				ks[l] = l + j < i ? is[l + j] : -2;
			}

			quickSort(0, i, intComparator, swapper);

			for(int l = 0; l < i; ++l) {
				if (l > 0 && js[l] == js[l - 1] && ks[l] == ks[l - 1]) {
					is[ls[l]] = is[ls[l - 1]];
				} else {
					is[ls[l]] = l;
				}
			}
		}

		List<Integer> intList = this.suffixIndexToObjectIndex;
		List<Integer> intList2 = this.offsetInText;
		this.suffixIndexToObjectIndex = new ArrayList<>(intList.size());
		this.offsetInText = new ArrayList<>(intList2.size());

		for(int m = 0; m < i; ++m) {
			int n = ls[m];
			this.suffixIndexToObjectIndex.add(intList.get(n));
			this.offsetInText.add(intList2.get(n));
		}

		if (PRINT_ARRAY) {
			this.printArray();
		}

	}

	private void printArray() {
		for(int i = 0; i < this.suffixIndexToObjectIndex.size(); ++i) {
			LOGGER.debug("{} {}", i, this.getDebugString(i));
		}

		LOGGER.debug("");
	}

	private String getDebugString(int suffixIndex) {
		int i = this.offsetInText.get(suffixIndex);
		int j = this.textStarts.get(this.suffixIndexToObjectIndex.get(suffixIndex));
		StringBuilder stringBuilder = new StringBuilder();

		for(int k = 0; j + k < this.characters.size(); ++k) {
			if (k == i) {
				stringBuilder.append('^');
			}

			int l = this.characters.get(j + k);
			if (l == -1) {
				break;
			}

			stringBuilder.append((char)l);
		}

		return stringBuilder.toString();
	}

	private int compare(String string, int suffixIndex) {
		int i = this.textStarts.get(this.suffixIndexToObjectIndex.get(suffixIndex));
		int j = this.offsetInText.get(suffixIndex);

		for(int k = 0; k < string.length(); ++k) {
			int l = this.characters.get(i + j + k);
			if (l == -1) {
				return 1;
			}

			char c = string.charAt(k);
			char d = (char)l;
			if (c < d) {
				return -1;
			}

			if (c > d) {
				return 1;
			}
		}

		return 0;
	}

	/**
	 * Retrieves all objects of which corresponding texts contain {@code text}.
	 * 
	 * <p>You have to call {@link #build()} method before calling this method.
	 * 
	 * <p>Takes O({@code text.length()} * log N) time to find objects where N is the
	 * sum of all text length added. Takes O(X + Y * log Y) time to collect found
	 * objects into a list where X is the number of occurrences of {@code text} in all
	 * texts added, and Y is the number of found objects.
	 */
	public List<T> findAll(String text) {
		int i = this.suffixIndexToObjectIndex.size();
		int j = 0;
		int k = i;

		while(j < k) {
			int l = j + (k - j) / 2;
			int m = this.compare(text, l);
			if (PRINT_COMPARISONS) {
				LOGGER.debug("comparing lower \"{}\" with {} \"{}\": {}", text, l, this.getDebugString(l), m);
			}

			if (m > 0) {
				j = l + 1;
			} else {
				k = l;
			}
		}

		if (j >= 0 && j < i) {
			int l = j;
			k = i;

			while(j < k) {
				int m = j + (k - j) / 2;
				int n = this.compare(text, m);
				if (PRINT_COMPARISONS) {
					LOGGER.debug("comparing upper \"{}\" with {} \"{}\": {}", text, m, this.getDebugString(m), n);
				}

				if (n >= 0) {
					j = m + 1;
				} else {
					k = m;
				}
			}

			int m = j;
			Set<Integer> intSet = new HashSet<>();

			for(int o = l; o < m; ++o) {
				intSet.add(this.suffixIndexToObjectIndex.get(o));
			}

			int[] is = intSet.stream().mapToInt(value -> value).toArray();
			java.util.Arrays.sort(is);
			Set<T> set = Sets.<T>newLinkedHashSet();

			for(int p : is) {
				set.add(this.objects.get(p));
			}

			return Lists.<T>newArrayList(set);
		} else {
			return Collections.emptyList();
		}
	}

	private static void quickSort(final int from, final int to, final Comparator<Integer> comp, final BiConsumer<Integer, Integer> swapper) {
		final int len = to - from;
		// Insertion sort on smallest arrays
		if (len < 16) {
			for (int i = from; i < to; i++)
				for (int j = i; j > from && (comp.compare(j - 1, j) > 0); j--) {
					swapper.accept(j, j - 1);
				}
			return;
		}

		// Choose a partition element, v
		int m = from + len / 2; // Small arrays, middle element
		int l = from;
		int n = to - 1;
		if (len > 128) { // Big arrays, pseudomedian of 9
			final int s = len / 8;
			l = med3(l, l + s, l + 2 * s, comp);
			m = med3(m - s, m, m + s, comp);
			n = med3(n - 2 * s, n - s, n, comp);
		}
		m = med3(l, m, n, comp); // Mid-size, med of 3
		// int v = x[m];

		int a = from;
		int b = a;
		int c = to - 1;
		// Establish Invariant: v* (<v)* (>v)* v*
		int d = c;
		while (true) {
			int comparison;
			while (b <= c && ((comparison = comp.compare(b, m)) <= 0)) {
				if (comparison == 0) {
					// Fix reference to pivot if necessary
					if (a == m) m = b;
					else if (b == m) m = a;
					swapper.accept(a++, b);
				}
				b++;
			}
			while (c >= b && ((comparison = comp.compare(c, m)) >= 0)) {
				if (comparison == 0) {
					// Fix reference to pivot if necessary
					if (c == m) m = d;
					else if (d == m) m = c;
					swapper.accept(c, d--);
				}
				c--;
			}
			if (b > c) break;
			// Fix reference to pivot if necessary
			if (b == m) m = d;
			else if (c == m) m = c;
			swapper.accept(b++, c--);
		}

		// Swap partition elements back to middle
		int s;
		s = Math.min(a - from, b - a);
		swap(swapper, from, b - s, s);
		s = Math.min(d - c, to - d - 1);
		swap(swapper, b, to - s, s);

		// Recursively sort non-partition-elements
		if ((s = b - a) > 1) quickSort(from, from + s, comp, swapper);
		if ((s = d - c) > 1) quickSort(to - s, to, comp, swapper);
	}

	private static int med3(final int a, final int b, final int c, final Comparator<Integer> comp) {
		final int ab = comp.compare(a, b);
		final int ac = comp.compare(a, c);
		final int bc = comp.compare(b, c);
		return (ab < 0 ?
				(bc < 0 ? b : ac < 0 ? c : a) :
				(bc > 0 ? b : ac > 0 ? c : a));
	}

	private static void swap(final BiConsumer<Integer, Integer> swapper, int a, int b, final int n) {
		for (int i = 0; i < n; i++, a++, b++) swapper.accept(a, b);
	}

}
