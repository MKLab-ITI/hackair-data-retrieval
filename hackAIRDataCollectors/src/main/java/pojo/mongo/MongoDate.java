package pojo.mongo;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MongoDate {

	@JsonProperty("$date")
	private long date;

	public MongoDate(Date datetime) {
		this.date = datetime.getTime();
	}

	public long getDate() {
		return date;
	}

	public void setDate(long date) {
		this.date = date;
	}

}
