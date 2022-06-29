package shyam.blood.donation;

public class AmbulanceModel {
    String id,name,phone,address;
    double distance;

    public AmbulanceModel() {
    }

    public AmbulanceModel(String id, String name, String phone, String address, double distance) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.address = address;
        this.distance = distance;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
