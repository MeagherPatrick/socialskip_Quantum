// TODO
// convert to sheets

package socialskip;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Iterator;
import java.util.stream.Collectors;

/**
 * Java class for communicating with the Fusion Table Service The code is based
 * on ApiExample.java from Kathryn Hurley (kbrisbin@google.com) Dependencies: -
 * GData Java Client Library - opencsv
 *
 */
public class SheetsApi {
	private static final String APPLICATION_NAME = "Google Sheets API Java Quickstart";
	private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
	private static final String TOKENS_DIRECTORY_PATH = "tokens";

	private static final XMLFileParser config;

	static {
		XMLFileParser tmp = null;

		tmp = new XMLFileParser("/config.xml");
		config = tmp;
	}

	public static final String RESEARCHERS = config.RESEARCHERS; // Researchers table ID
	public static final String EXPERIMENTS = config.EXPERIMENTS; // Experiments table ID
	public static final String INTERACTIONS = config.INTERACTIONS; // Interactions table ID
	public static final String DOWNLOAD = config.MERGE_INTERACTIONS_TRANSACTIONS;    // Merge of Interactions and Transactions
	public static final String ACCESS_TOKENS = config.ACCESS_TOKENS;    // Access Tokens

	private HttpTransport HTTP_TRANSPORT;

	private QueryResults last;

	/**
	 * Global instance of the scopes required by this quickstart.
	 * If modifying these scopes, delete your previously saved tokens/ folder.
	 */
	private static final List<String> SCOPES =
			Collections.singletonList(SheetsScopes.SPREADSHEETS);
	private static final String CREDENTIALS_FILE_PATH = "/credentials.json.json";

	/**
	 * Creates an authorized Credential object.
	 *
	 * @param HTTP_TRANSPORT The network HTTP Transport.
	 * @return An authorized Credential object.
	 * @throws IOException If the credentials.json.json file cannot be found.
	 */
	private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT)
			throws IOException {
		// Load client secrets.
		InputStream in = SheetsApi.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
		if (in == null) {
			throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
		}
		GoogleClientSecrets clientSecrets =
				GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

		// Build flow and trigger user authorization request.
		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
				HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
				.setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
				.setAccessType("offline")
				.build();
		LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
		return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
	}

	/**
	 * Prints the names and majors of students in a sample spreadsheet:
	 * https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms/edit
	 */
	public void getDataFromSheet(String spreadsheetId, String range) throws IOException, GeneralSecurityException {
		// Retrieve results using getResults method
		QueryResults queryResults = getResults(spreadsheetId, range);

		// Store the results in the 'last' variable
		if (queryResults.getColumnNames().isEmpty() && queryResults.getRows().isEmpty()) {
			last = null;
		} else {
			last = queryResults;
		}

	}

	public QueryResults getResults(String spreadsheetId, String range) throws IOException, GeneralSecurityException {
		final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
		Sheets service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
				.setApplicationName(APPLICATION_NAME)
				.build();

		ValueRange response = service.spreadsheets().values()
				.get(spreadsheetId, range)
				.execute();

		List<List<Object>> values = response.getValues();
		List<String> columnNames = new ArrayList<>();
		List<String[]> rows = new ArrayList<>();

		if (values == null || values.isEmpty()) {
			System.out.println("No data found.");
		} else {
			// Extracting column names
			List<Object> header = values.get(0);
			columnNames = header.stream()
					.map(Object::toString)
					.collect(Collectors.toList());

			// Extracting row values (excluding the first row which contains column names)
			for (int i = 1; i < values.size(); i++) {
				List<Object> row = values.get(i);
				String[] rowArray = row.stream()
						.map(Object::toString)
						.toArray(String[]::new);
				rows.add(rowArray);
			}
		}

		return new QueryResults(columnNames, rows);
	}


	/**
	 * Print the results of the last query.
	 */
	public void print() {
		last.print();
	}

	/**
	 * Returns an Iterator over the results of the last query
	 *
	 * @return Iterator
	 */
	public Iterator<String[]> getRowsIterator() {
		return last.getRowsIterator();
	}

	/**
	 * Returns the number of rows of the last query
	 *
	 * @return Number of rows
	 */
	public int rowCount() {
		return last.rows.size();
	}

	/**
	 * Returns the first row of the last query
	 *
	 * @return First row
	 */
	public String[] getFirstRow() throws IndexOutOfBoundsException {
		return last.rows.get(0);
	}

	/**
	 * Returns an array containing the column names of the last query
	 *
	 * @return Array of strings
	 */
	public String[] getColumnNames() {
		return last.columnNames.toArray(new String[0]);
	}

	/**
	 * Result of a Fusion Table query.
	 */
	class QueryResults {
		final List<String> columnNames;
		final List<String[]> rows;

		public QueryResults(List<String> columnNames, List<String[]> rows) {
			this.columnNames = columnNames;
			this.rows = rows;
		}

		/**
		 * Returns an iterator over result rows
		 *
		 * @return Iterator
		 */
		public Iterator<String[]> getRowsIterator() {
			return rows.iterator();
		}

		/**
		 * Returns the column name
		 *
		 * @return columnNames
		 */
		public List<String> getColumnNames(){
			return columnNames;
		}

		/**
		 * Prints the results of the query.
		 *
		 */
		public void print() {
			String sep = "";
			for (int i = 0; i < columnNames.size(); i++) {
				System.out.print(sep + columnNames.get(i));
				sep = ", ";
			}
			System.out.println();

			for (int i = 0; i < rows.size(); i++) {
				String[] rowValues = rows.get(i);
				sep = "";
				for (int j = 0; j < rowValues.length; j++) {
					System.out.print(sep + rowValues[j]);
					sep = ", ";
				}
				System.out.println();
			}
		}

		public List<String[]> getRows() {
			return rows;
		}
	}
// TODO
// /**
// 	 * Executes a Fusion Tables SQL query and store the results.
// 	 */
// 	public void run(String query) throws IOException, SocketTimeoutException, GeneralSecurityException {

// 	    Sql sql = fusiontables.query().sql(query);

// 	    Sqlresponse response = sql.execute();

// 	    last = getResults(response);

// 	}
 }
