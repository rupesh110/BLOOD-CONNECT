package shyam.blood.donation;

public class BloodBankModel {
    String name,address,phone, id,google;
    double distance;

    public BloodBankModel() {
    }

    public BloodBankModel(String id,String name, String address, String phone,String google,double distance) {
       this.id = id;
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.google = google;
        this.distance = distance;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public String getGoogle() {
        return google;
    }

    public void setGoogle(String google) {
        this.google = google;
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }


}
