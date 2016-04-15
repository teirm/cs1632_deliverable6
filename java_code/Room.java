public class Room {
	
	private String northDoor;
	private String southDoor;
	private String furniture;
	private String item;
	private String roomAdj;
	
	//setters

	public void setRoomAdj(String s) {
		this.roomAdj = s;
	}

	public void setNorthDoor(String s) {
		this.northDoor = s;
	}

	public void setSouthDoor(String s) {
		this.southDoor = s;
	}

	public void setFurniture(String s) {
		this.furniture = s;
	}

	public void setItem(String s) {
		this.item = s;
	}


	//getters
	public String getRoomAdj() {
		return roomAdj;
	}	
	
	public String getNorthDoor() {
		return northDoor;
	}

	public String getSouthDoor() {
		return southDoor;
	}

	public String getFurniture() {
		return furniture;
	}

	public String getItem() {
		return item;
	}

}
