package gr.iti.mklab.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;

import org.bson.Document;
import org.json.JSONObject;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class MongoConnection {

	private String username;
	private String password;
	private String host;
	private int port;
	private String authMechanism;
	private String databaseName;

	public String getCollectionName() {
		return collectionName;
	}

	private String collectionName;
	private MongoClient mongoClient;

	public MongoConnection(String settingsFile) throws Exception {
		// initialize mongo connection settings from the settings file
		StringBuffer sb = new StringBuffer();
		String line;
		BufferedReader in = new BufferedReader(new FileReader(new File(settingsFile)));
		while ((line = in.readLine()) != null) {
			sb.append(line);
		}
		in.close();
		JSONObject json = new JSONObject(sb.toString());
		JSONObject mongoSettingsObj = (JSONObject) json.getJSONArray("mongo_settings").get(0);

		host = mongoSettingsObj.getString("host");
		port = mongoSettingsObj.getInt("port");
		authMechanism = mongoSettingsObj.getString("authMechanism");
		username = mongoSettingsObj.getString("username");
		password = mongoSettingsObj.getString("password");
		databaseName = mongoSettingsObj.getString("databaseName");
		collectionName = mongoSettingsObj.getString("collectionName");

		mongoClient = connectToMongo(host, port, authMechanism, username, password, databaseName);
	}

	public MongoClient getMongoClient() {
		return mongoClient;
	}

	public MongoConnection(String databaseName, String collectionName) {
		// set default values
		host = "";
		port = 27017;
		authMechanism = "";
		username = "";
		password = "";
		this.databaseName = databaseName;
		this.collectionName = collectionName;
		mongoClient = connectToMongo(host, port, authMechanism, username, password, databaseName);
	}

	public MongoConnection(String databaseName, String collectionName, String host) {
		// set default values
		this.host = host;
		port = 27017;
		authMechanism = "";
		username = "";
		password = "";
		this.databaseName = databaseName;
		this.collectionName = collectionName;
		mongoClient = connectToMongo(host, port, authMechanism, username, password, databaseName);
	}

	public static MongoClient connectToMongo(String host, int port, String authMechanism, String username,
			String password, String databaseName) {

		MongoClient mongoClient = null;
		if (authMechanism.equals("")) {
			if (host.equals("")) {
				host = "localhost";
				port = 27017;
			}
			mongoClient = new MongoClient(new ServerAddress(host, port), MongoClientOptions.builder()
					.codecRegistry(com.mongodb.MongoClient.getDefaultCodecRegistry()).build());
		} else {
			if (authMechanism.equalsIgnoreCase("MONGODB-CR")) {
				MongoCredential credential1 = MongoCredential.createMongoCRCredential(username, databaseName,
						password.toCharArray());
				mongoClient = new MongoClient(new ServerAddress(host, port), Arrays.asList(credential1),
						MongoClientOptions.builder()
								.codecRegistry(com.mongodb.MongoClient.getDefaultCodecRegistry()).build());
			}

			if (authMechanism.equalsIgnoreCase("SCRAM-SHA-1")) {
				MongoCredential credential2 = MongoCredential.createScramSha1Credential(username, "admin",
						password.toCharArray());
				MongoClientOptions options = MongoClientOptions.builder()
						.writeConcern(WriteConcern.UNACKNOWLEDGED)
						.codecRegistry(com.mongodb.MongoClient.getDefaultCodecRegistry()).build();

				mongoClient = new MongoClient(new ServerAddress(host, port), Arrays.asList(credential2),
						options);
			}
		}
		return mongoClient;
	}

	public MongoDatabase getDb() {
		MongoDatabase db = mongoClient.getDatabase(databaseName);
		return db;
	}

	public DB getDbOld() {
		DB db = mongoClient.getDB(databaseName);
		return db;
	}

	public MongoCollection<Document> getCollection() {
		MongoCollection<Document> collection = mongoClient.getDatabase(databaseName)
				.getCollection(collectionName);
		return collection;
	}

	public MongoCollection<Document> getCollection(String collectionName) {
		MongoCollection<Document> collection = mongoClient.getDatabase(databaseName)
				.getCollection(collectionName);
		return collection;
	}

	public void closeConnectionToMongo() {
		mongoClient.close();
	}

}
