package dto;

/** Thống kê suất chiếu và mức lấp đầy ghế theo một nhóm (phim, chi nhánh, phòng, ngày). */
public class OccupancyRow {

    private int id;
    private String name;
    private String subName;
    private int shows;
    private int soldSeats;
    private int totalSeats;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSubName() { return subName; }
    public void setSubName(String subName) { this.subName = subName; }

    public int getShows() { return shows; }
    public void setShows(int shows) { this.shows = shows; }

    public int getSoldSeats() { return soldSeats; }
    public void setSoldSeats(int soldSeats) { this.soldSeats = soldSeats; }

    public int getTotalSeats() { return totalSeats; }
    public void setTotalSeats(int totalSeats) { this.totalSeats = totalSeats; }

    /** Tỷ lệ lấp đầy (%) = ghế đã bán / tổng ghế khả dụng của các suất chiếu. */
    public double getOccupancy() {
        return totalSeats > 0 ? soldSeats * 100.0 / totalSeats : 0;
    }

    /** Tỷ lệ ghế trống (%). */
    public double getVacancy() {
        return totalSeats > 0 ? 100.0 - getOccupancy() : 0;
    }
}
