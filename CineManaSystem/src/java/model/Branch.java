package model;

public class Branch {
    private int branchID;
    private String name;
    private String address;
    private String city;
    private String hotline;

    public Branch() {
    }

    public Branch(int branchID, String name, String address, String city, String hotline) {
        this.branchID = branchID;
        this.name = name;
        this.address = address;
        this.city = city;
        this.hotline = hotline;
    }

    public int getBranchID() {
        return branchID;
    }

    public void setBranchID(int branchID) {
        this.branchID = branchID;
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

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getHotline() {
        return hotline;
    }

    public void setHotline(String hotline) {
        this.hotline = hotline;
    }
}
