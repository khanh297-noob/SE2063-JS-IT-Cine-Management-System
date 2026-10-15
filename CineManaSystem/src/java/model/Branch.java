package model;

public class Branch {
    private int branchId;
    private String name;
    private String address;
    private String city;
    private String hotline;

    public Branch() {
    }

    public Branch(int branchId, String name, String address, String city, String hotline) {
        this.branchId = branchId;
        this.name = name;
        this.address = address;
        this.city = city;
        this.hotline = hotline;
    }

    public int getBranchId() {
        return branchId;
    }

    public void setBranchId(int branchId) {
        this.branchId = branchId;
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