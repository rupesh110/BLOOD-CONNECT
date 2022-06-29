package shyam.blood.donation;

public class BloodRequestModel {
private String rname, raddress, rphone,rnote,rblood, rpint,id,date;

    public BloodRequestModel() {
    }

    public BloodRequestModel(String id,String rname, String raddress, String rphone, String rnote, String rblood, String rpint,String date) {
        this.id = id;
        this.rname = rname;
        this.raddress = raddress;
        this.rphone = rphone;
        this.rnote = rnote;
        this.rblood = rblood;
        this.rpint = rpint;
        this.date = date;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRname() {
        return rname;
    }

    public void setRname(String rname) {
        this.rname = rname;
    }

    public String getRaddress() {
        return raddress;
    }

    public void setRaddress(String raddress) {
        this.raddress = raddress;
    }

    public String getRphone() {
        return rphone;
    }

    public void setRphone(String rphone) {
        this.rphone = rphone;
    }

    public String getRnote() {
        return rnote;
    }

    public void setRnote(String rnote) {
        this.rnote = rnote;
    }

    public String getRblood() {
        return rblood;
    }

    public void setRblood(String rblood) {
        this.rblood = rblood;
    }

    public String getRpint() {
        return rpint;
    }

    public void setRpint(String rpint) {
        this.rpint = rpint;
    }
}
