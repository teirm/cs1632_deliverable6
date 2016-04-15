public class MyDumbRoom {

	String dumbFurniture;
	String dumbItem;
	String n_door;
	String s_door;


	public void setFurniture(String antique) {
		dumbFurniture = antique;
	}

	public void setItem(String junk) {
	   dumbItem = junk;
	}	   

	public void setNorthDoor(String north) {
		n_door = north;
	}	

	public void setSouthDoor(String south) {
		s_door = south;
	}

	public String getNorthDoor() {
		return n_door;
	}

	public String getSouthDoor() {
		return s_door;	
	}

	public String getFurniture() {
		return dumbFurniture;
	}

	public String getItem() {
		return dumbItem;
	}
}	
