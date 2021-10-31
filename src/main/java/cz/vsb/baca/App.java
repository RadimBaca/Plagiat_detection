package cz.vsb.baca;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.io.inputstream.ZipInputStream;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.UnzipParameters;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import net.lingala.zip4j.util.FileUtils;

import net.lingala.zip4j.ZipFile;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import java.sql.*;


public class App {

	public static class Pair
	{
		private final String key;
		private final Double value;

		public Pair(String aKey, Double aValue)
		{
			key   = aKey;
			value = aValue;
		}

		public String key()   { return key; }
		public Double value() { return value; }
	}
	
	private static final String SQL_INSERT = "INSERT INTO projekty VALUES (?,?, ?)";
	private static final String SQL_INSERT_PODOBNE = "INSERT INTO podobne_projekty VALUES (?,?, ?)";
	private static final String SQL_SELECT = "SELECT login, file_name, similarity(doc, ?) sim FROM projekty WHERE login != ? and similarity(doc, ?) > 0.4 ORDER BY sim desc LIMIT 10";
	
	public static void main(String[] args)  {
		if (args.length < 1) {
			System.out.println("App [-i|-q] dir user password");
			System.exit(1);
		}
		
		if (args[0].equals("-i"))
		{
			insertIntoTable(args[1], args[2], args[3]);
		}
		if (args[0].equals("-q"))
		{
			processQueries(args[1], args[2], args[3]);
		}		
	}

	
	public static void processQueries(String dirName, String user, String pass)
	{
        try (Connection conn = DriverManager.getConnection(
                "jdbc:postgresql://bayer.cs.vsb.cz:5432/projekty", user, pass)) {

            if (conn != null) {
                System.out.println("Connected to the database!");
            } else {
                System.out.println("Failed to make connection!");
            }
					
			File dir = new File(dirName);
			File[] filesList = dir.listFiles();
			for (File file : filesList) {
				if (file.isFile()) {
					System.out.println(file.getName());
					String fileName = dirName + "/" + file.getName();
					String doc = parseToString(fileName);
					PreparedStatement psSelect = conn.prepareStatement(SQL_SELECT);
					psSelect.setString(1, doc);	
					psSelect.setString(2, file.getName().substring(0,7));
					psSelect.setString(3, doc);	
					
					ResultSet resultSet = psSelect.executeQuery();
					ArrayList<Pair> similar_doc = new ArrayList<Pair>();
					while (resultSet.next()) {
						
						String login = resultSet.getString("login");
						String similar_file_name = resultSet.getString("file_name");
						double sim = resultSet.getDouble("sim");
						System.out.println("  " + similar_file_name + " - " + sim);
						
						Pair newpair = new Pair(similar_file_name, sim);
						similar_doc.add(newpair);						
					}		
					similar_doc.forEach((n) -> {
						try {
							PreparedStatement psInsert = conn.prepareStatement(SQL_INSERT_PODOBNE);
							psInsert.setString(1, file.getName());	
							psInsert.setString(2, n.key());	
							psInsert.setDouble(3, n.value());
							int rows = psInsert.executeUpdate();
						} catch (SQLException e) {
							System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
						} catch (Exception e) {
							e.printStackTrace();
						}							
					});
					
					System.out.println("--------------\n");
				}
			}
        } catch (SQLException e) {
            System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }		
	}
	
	public static void insertIntoTable(String dirName, String user, String pass)
	{
        try (Connection conn = DriverManager.getConnection(
                "jdbc:postgresql://bayer.cs.vsb.cz:5432/projekty", user, pass)) {

            if (conn != null) {
                System.out.println("Connected to the database!");
            } else {
                System.out.println("Failed to make connection!");
            }
					
			File dir = new File(dirName);
			File[] filesList = dir.listFiles();
			for (File file : filesList) {
				if (file.isFile()) {
					System.out.println(file.getName());
					String fileName = dirName + "/" + file.getName();
					if (file.getName().substring(file.getName().length() - 4, file.getName().length()).equals(".zip"))
					{
						Path source = Paths.get(fileName);
						Path dest = Paths.get(dirName);
						unzipFolderZip4j(source, dest);
						// this command assumes that the zip file contains a pdf of the same name as zip
						fileName = dirName + "/" + file.getName().substring(0, file.getName().length() - 4) + ".pdf";
					}
					String doc = parseToString(fileName);
					
					PreparedStatement psInsert = conn.prepareStatement(SQL_INSERT);
		            psInsert.setString(1, file.getName().substring(0,7));	
					psInsert.setString(2, file.getName());
					psInsert.setString(3, doc);
		            int rows = psInsert.executeUpdate();
					//System.out.println(" - " + Arrays.toString(rows));			
					
				}
			}
        } catch (SQLException e) {
            System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }		
	}
	
	public static String parseToString(String fileName) throws IOException, SAXException, TikaException {
		File file = new File(fileName);

		AutoDetectParser parser = new AutoDetectParser();
		InputStream stream = new FileInputStream(file);
		BodyContentHandler handler = new BodyContentHandler();
		Metadata metadata = new Metadata();
		ParseContext parseContext = new ParseContext();

		parser.parse(stream, handler, metadata, parseContext);

		String content = handler.toString();
		return content;
	}  
	
	public static void unzipFolderZip4j(Path source, Path target)
		throws IOException {

		new ZipFile(source.toFile())
				.extractAll(target.toString());

	}	
}