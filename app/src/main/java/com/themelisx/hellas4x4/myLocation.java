package com.themelisx.hellas4x4;

public class myLocation {
	
	private long id;
	private String title;
	private String note;
	private String address;
	private double lat;
	private double lon;
	private double alt;
	private String accuracy;
	private int category;


    myLocation(){
        super();
    }
    
	public myLocation(long id, String title, String note, String datetime, String address, double lat, double lon, double alt, String accuracy, int category ) {
	    super();
	    
	    this.id = id;
	    this.title = title;
	    this.note = note;
	    this.address = address;
	    this.lat = lat;
	    this.lon = lon;
	    this.alt = alt;
	    this.accuracy = accuracy;
	    this.category = category;
	}
	
	public long getId() {
		  return id;
		}
	
	public String getAddress() {
		  return address;
		}
	
	public int getCategory() {
		  return category;
		}
	
	public String getTitle() {
		  return title;
		}
	
	public String getNote() {
		  return note;
		}

	public double getAlt() {
		  return alt;
		}
	
	public double getLat() {
		  return lat;
		}
	
	public double getLon() {
		  return lon;
		}

	public String getAccuracy() { return accuracy; }
	
	public void setId(long id) {
		  this.id = id;
	}
	
	public void setAddress(String address) {
		  this.address = address;
	}
	
	public void setCategory(int category) {
		  this.category = category;
	}
	
	public void setTitle(String title) {
		  this.title = title;
	}
	
	public void setNote(String note) {
		  this.note = note;
	}
	
	public void setAlt(double alt) {
		  this.alt = alt;
	}
	
	public void setLat(double lat) { this.lat = lat; }
	
	public void setLon(double lon) {
		  this.lon = lon;
	}

	public void setAccuracy(String accuracy) { this.accuracy = accuracy; }

}
