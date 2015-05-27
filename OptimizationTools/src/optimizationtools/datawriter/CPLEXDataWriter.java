package optimizationtools.datawriter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * An auxiliary class to create data files for CPLEX models.
 * 
 * <h3>Example:</h3>
 * 
 * <p>
 * To use the {@code CPLEXDataWriter}, it is convenient to subclass it, as you can then use its utility methods without
 * having to specify an instance name or a class name.
 * </p>
 * 
 * Consider the following example class:
 * 
 * <pre>
 * <b>class</b> CPLEXDataFormatter <b>extends</b> CPLEXDataWriter {
 *     <b>private</b> {@link HashSet}{@literal <MyDataObject>} dataItems;
 *     
 *     <b>public</b> CPLEXDataFormatter(HashSet{@literal <MyDataObject>} dataItems) { this.dataItems = dataItems; }
 * 
 *     <i>{@literal @}Override</i>
 *     <b>public</b> String writeDataFileToString() {
 *         // add primitive constants
 *         {@link #addElement(ICPLEXElement) addElement}(new {@link CPLEXConstant}{@literal <String>}("dataTitle", "test name");
 *         addElement(new CPLEXConstant{@literal <Integer>}("timeHorizon", 4));
 *         addElement(new {@link CPLEXFloatConstant}("stepLength", 15.0));
 *         
 *         // generate item IDs
 *         {@link Hashtable}<MyDataObject, String> dataItemIDs = {@link #enumerateItemIDs(Collection, String) enumerateItemIDs}(dataItems, "di");
 *         
 *         // add the ID set
 *         addElement(new {@link CPLEXSet}{@literal <String>}("dataItemIDs", dataItemIDs.{@link Hashtable#values() values}()));
 *         
 *         // add the tuple sets with object data
 *         addElement({@link #createTupleArray(String, Map, ICPLEXTupleMapper) createTupleArray}("myData", dataItemIDs, new MyDataMapper()));
 *     }
 *     
 *     <b>private static class</b> MyDataMapper <b>implements</b> {@link ICPLEXTupleMapper}{@literal <MyDataObject>} {
 *         <i>{@literal @}Override</i>
 *         <b>public</b> {@link List}{@literal <}{@link ICPLEXElement}{@literal >} getValueList(MyDataObject obj) {
 *             {@link LinkedList}{@literal <CPLEXElement>} result = new LinkedList{@literal <CPLEXElement>}();
 *             // add all the attributes to the list of CPLEXElements.
 *             // if they are already present in obj or can be derived with its getters, that is easy
 *             result.add(new CPLEXFloatConstant("productionCost", obj.getProductionCost()));
 *             result.add(new CPLEXFloatConstant("recyclingCost", obj.getProductionCost() * MyDataObject.RECYCLE_COST_FACTOR));
 *             result.add(new CPLEXFloatConstant("transportCost", obj.getTransportCost()));
 *             // if you need to do more analysis and processing to derive some attributes of obj, you can also do that here.
 *             // you could even give <b>MyDataMapper</b> some fields that you initialize in its constructor (possibly passing in
 *             // other objects with additional information)
 *             <b>return</b> result;
 *         }
 *     }
 * }
 * </pre>
 * 
 * @author lehnepat
 * 
 */
public class CPLEXDataWriter {

	protected ArrayList<ICPLEXElement> elements = new ArrayList<ICPLEXElement>();
	protected String[] prefixText = null;
	private boolean prettyPrintingEnabled = true;

	private void writeDataFileInternal(File outFile) throws IOException {
		Writer dataFileWriter = null;
		try {
			dataFileWriter = new BufferedWriter(new FileWriter(outFile));
			dataFileWriter.write(writeDataFileToString());
		} finally {
			if (dataFileWriter != null)
				dataFileWriter.close();
		}
	}

	/**
	 * Write all elements added to this datawriter up to now to a temporary file. The path and filename of this
	 * temporary file is generated by {@link File#createTempFile(String, String)
	 * File.createTempFile("networkstructure-data-", ".dat")}.
	 * 
	 * @see #writeDataFileToString()
	 * @see #writeDataFile(String)
	 * @see #writeDataFileToTempFile(String)
	 * 
	 * @return the absolute path and filename generated for the temporary file
	 * @throws IOException
	 *             if an error occurs while writing the file to disk
	 */
	public String writeDataFileToTempFile() throws IOException {
		return writeDatafileToTempFile("networkstructure-data-");
	}

	/**
	 * Write all elements added to this datawriter up to now to a temporary file. The path and filename of this
	 * temporary file is generated by {@linkplain File#createTempFile(String, String) <code>File.createTempFile(</code>
	 * <b>prefix</b><code>, ".dat")</code>}.
	 * 
	 * @see #writeDataFileToString()
	 * @see #writeDataFile(String)
	 * @see #writeDataFileToTempFile()
	 * 
	 * @param prefix
	 *            the prefix to use for generating the name of the temporary file
	 * @return the absolute path and filename generated for the temporary file
	 * @throws IOException
	 *             if an error occurs while writing the file to disk
	 */
	public String writeDatafileToTempFile(final String prefix) throws IOException {
		File dataFile = File.createTempFile(prefix, ".dat");
		writeDataFileInternal(dataFile);
		return dataFile.getAbsolutePath();
	}

	/**
	 * Write all elements added to this datawriter up to now to the file specified by <b>filename</b>. If the file does
	 * not exist, it is created. If it does exist, it is overwritten without asking for confirmation.
	 * 
	 * @see #writeDataFileToString()
	 * 
	 * @param filename
	 *            the path and filename to save the data file to
	 * @throws IOException
	 *             if an error occurs while writing the file to disk
	 */
	public void writeDataFile(String filename) throws IOException {
		writeDataFileInternal(new File(filename));
	}

	/**
	 * Write all elements added to this datawriter up to now to a String. If pretty printing is enabled, those elements
	 * that support it will be pretty-printed. If a prefix text is set, it is inserted into the header comment of the
	 * data file.
	 * 
	 * @see #isPrettyPrintingEnabled()
	 * @see #setPrettyPrintingEnabled(boolean)
	 * @see #getPrefixText()
	 * @see #setPrefixText(String)
	 * @see #setPrefixText(String[])
	 * 
	 * @return the formatted CPLEX data file
	 */
	public String writeDataFileToString() {
		StringBuffer fileContents = new StringBuffer();

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss zzz");
		fileContents.append("/******************************************************\n");
		fileContents.append(" * Auto generated data file\n");
		if (prefixText != null && prefixText.length > 0)
			for (String l : prefixText)
				fileContents.append(" * ").append(l).append('\n');
		fileContents.append(" * Creation Date: ");
		fileContents.append(sdf.format(new Date()));
		fileContents.append("\n");
		fileContents.append(" ******************************************************/\n\n");

		for (ICPLEXElement ce : elements) {
			fileContents.append(ce.getName()).append(" = ");
			fileContents.append(ce.getContent(prettyPrintingEnabled, 4, 4).toString());
			fileContents.append(";\n\n");
		}

		return fileContents.toString();
	}

	/**
	 * Append <b>element</b> to the list of elements of this datawriter.
	 * 
	 * @param element
	 */
	public void addElement(ICPLEXElement element) {
		elements.add(element);
	}

	/**
	 * Get the current prefix text (defaults to {@code null}).
	 * 
	 * @return the current prefix text (defaults to {@code null}).
	 * 
	 * @see #setPrefixText(String[])
	 */
	public String[] getPrefixText() {
		return prefixText;
	}

	/**
	 * <p>
	 * Set the prefix text for this datawriter. The prefix text is spliced into the header comment of the CPLEX data
	 * file. To disable the inclusion of any additional prefix text, set this to {@code null}.
	 * </p>
	 * 
	 * <p>
	 * Each element of <b>prefixText</b> is started on a new line of the header comment. For nice formatting, each line
	 * should not exceed ca 55-60 characters. Each element is be prepended by the string " * " (no quotes).
	 * </p>
	 * 
	 * <p>
	 * <b>IMPORTANT:</b> Do not include the string "{@literal *}{@literal /}" (asterisk, forward slash) anywhere in
	 * <b>prefixText</b>, as it would end the header comment prematurely and break the data file.
	 * </p>
	 * 
	 * @param prefixText
	 *            additional comments to include in the header comment of the data file, one line per element; or
	 *            {@code null} to disable additional comments
	 */
	public void setPrefixText(String[] prefixText) {
		this.prefixText = prefixText;
	}

	/**
	 * <p>
	 * Set the prefix text for this datawriter to a single string. The prefix text is spliced into the header comment of
	 * the CPLEX data file. To disable the inclusion of any additional prefix text, set this to {@code null}.
	 * </p>
	 * 
	 * <p>
	 * <b>IMPORTANT:</b> Do not include the string "{@literal *}{@literal /}" (asterisk, forward slash) anywhere in
	 * <b>prefixText</b>, as it would end the header comment prematurely and break the data file.
	 * </p>
	 * 
	 * @param prefixText
	 *            additional comment to include in the header comment of the data file; or {@code null} to disable
	 *            additional comments
	 * 
	 * @see #setPrefixText(String[])
	 */
	public void setPrefixText(String prefixText) {
		if (prefixText == null)
			this.prefixText = null;
		else
			this.prefixText = new String[] { prefixText };
	}

	public boolean isPrettyPrintingEnabled() {
		return prettyPrintingEnabled;
	}

	public void setPrettyPrintingEnabled(boolean prettyPrintingEnabled) {
		this.prettyPrintingEnabled = prettyPrintingEnabled;
	}

	/**
	 * <p>
	 * Generate IDs for <b>items</b> by using a counter and an optional prefix string.
	 * </p>
	 * 
	 * <p>
	 * The generated IDs are of the format {@code <idPrefix>_<counterValue>}; i.e. the value of <b>idPrefix</b>,
	 * followed by an underscore, followed by the counter value (the counter is incremented for every element in
	 * <b>items</b>) formatted to a width of at least 4 digits with leading zeroes.
	 * </p>
	 * 
	 * @param items
	 *            the items to enumerate
	 * @param idPrefix
	 *            the prefix for the ID strings, set to the empty string to disable
	 * @return a {@link Hashtable} which maps each object in <b>items</b> to its generated ID string
	 */
	public static <T> Hashtable<T, String> enumerateItemIDs(Collection<T> items, final String idPrefix) {
		return generateItemIDs(items, new IIdentifierGenerator<T>() {
			@Override
			public String generateIdentifier(final T obj, final int runningCounter) {
				return String.format("%s_%04d", idPrefix, runningCounter);
			}
		});
	}

	/**
	 * <p>
	 * Generate IDs for <b>items</b> by using a custom identifier generator <b>ig</b>.
	 * </p>
	 * 
	 * <p>
	 * The method iterates over all elements of <b>items</b> and calls <b>ig</b>'s
	 * {@link IIdentifierGenerator#generateIdentifier(Object, int) generateIdentifier(Object, int)} for each of them,
	 * providing:
	 * <ul>
	 * <li>the element itself as an argument so the function can access it and use element attributes in the generation
	 * of IDs, and</li>
	 * <li>the value of a running counter, which is a unique value of every call to the function (it is not necessarily
	 * unique per object, as <b>items</b> may contain duplicates -- it is only unique per <i>object occurrence</i> in
	 * the collection).</li>
	 * </ul>
	 * {@code generateIdentifier(Object, int)} must return a unique valid CPLEX identifier for each unique object in
	 * <b>items</b>.
	 * </p>
	 * 
	 * @param items
	 *            the items for which to generate IDs
	 * @param ig
	 *            the identifier generator object
	 * @return a {@link Hashtable} which maps each object in <b>items</b> to its generated ID string
	 */
	public static <T> Hashtable<T, String> generateItemIDs(Collection<T> items, IIdentifierGenerator<? super T> ig) {
		Hashtable<T, String> result = new Hashtable<T, String>(items.size());
		int idCounter = 0;
		for (T item : items) {
			result.put(item, ig.generateIdentifier(item, idCounter++));
		}
		return result;
	}

	/**
	 * Create an (indexed) array of tuples with the given <b>name</b> from the given <b>items</b>, using the provided
	 * <b>mapper</b> to map each element of <b>items</b> to a list of {@link ICPLEXElement}s that get wrapped in a
	 * {@link CPLEXNamedTuple} each.
	 * 
	 * @see ICPLEXTupleMapper#getValueList(T)
	 * 
	 * @param name
	 *            the name for this array
	 * @param items
	 *            the list of items to include in this tuple array, mapped to their corresponding ID strings
	 * @param mapper
	 *            the {@link ICPLEXTupleMapper} to transform each element of <b>items</b> to a list of
	 *            {@code CPLEXElement}s
	 * @return an array of tuples in which each tuple corresponds to one entry of <b>items</b>
	 */
	public static <T> CPLEXIndexedArray createTupleArray(String name, final Map<T, String> items, final ICPLEXTupleMapper<T> mapper) {
		return createIndexedArray(name, items, new ICPLEXElementMapper<T, CPLEXNamedTuple>() {
			@Override
			public CPLEXNamedTuple getElement(T obj) {
				return new CPLEXNamedTuple(items.get(obj), mapper.getValueList(obj));
			}
		});
	}

	/**
	 * Create an indexed array of {@link ICPLEXElement}s with the given <b>name</b> from the given <b>items</b> by using
	 * the provided <b>mapper</b> to map each element of <b>items</b> to a {@code CPLEXElement} instance. This method
	 * does not check the elements created by <b>mapper</b> for type correctness (usually all elements of an array are
	 * of the same type).
	 * 
	 * @see ICPLEXElementMapper#getElement(T)
	 * 
	 * @param name
	 *            the name for this array
	 * @param items
	 *            the list of items to include this indexed array, mapped to their corresponding ID strings
	 * @param mapper
	 *            the {@link ICPLEXElementMapper} to transform each element of <b>items</b> to a {@code CPLEXElement}
	 * @return an array of {@code CPLEXElement}s in which each element corresponds to an entry of <b>items</b>
	 */
	public static <T, E extends ICPLEXElement> CPLEXIndexedArray createIndexedArray(String name, Map<T, String> items, ICPLEXElementMapper<T, E> mapper) {
		CPLEXIndexedArray arr = new CPLEXIndexedArray(name);
		List<ICPLEXElement> arrItems = arr.getItems();

		for (Entry<T, String> e : items.entrySet()) {
			E element = mapper.getElement(e.getKey());
			element.setName(e.getValue());
			arrItems.add(element);
		}

		return arr;
	}
}
