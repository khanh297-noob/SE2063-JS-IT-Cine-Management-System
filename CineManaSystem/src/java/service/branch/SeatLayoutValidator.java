package service.branch;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Kiểm tra hình dạng sơ đồ ghế. Ma trận grid[r][c] = seat_type_id của ghế, 0 = lối đi
 * (r, c tính từ 0; khi báo lỗi đổi sang nhãn A1, B7...).
 */
final class SeatLayoutValidator {

    /** Số ô tối đa liệt kê trong một thông báo lỗi, tránh thông báo quá dài. */
    private static final int MAX_LISTED = 8;
    private static final int[][] DIRECTIONS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

    private SeatLayoutValidator() {}

    /**
     * BR-SM-05  Lối đi phải xuất phát từ góc phòng: mọi ô lối đi phải nối liền (theo 4 hướng
     * trên/dưới/trái/phải) với ít nhất một trong 4 góc, và góc đó cũng phải là lối đi.
     *
     * BFS nhiều nguồn: đưa các góc là lối đi vào hàng đợi, loang sang các ô lối đi kề nhau.
     * Ô lối đi nào không được thăm là ô bị cô lập, không đi ra được góc nào.
     */
    static List<String> checkAisles(int[][] grid) {
        int rows = grid.length;
        int cols = grid[0].length;
        boolean[][] visited = new boolean[rows][cols];
        ArrayDeque<int[]> queue = new ArrayDeque<>();

        int[][] corners = {{0, 0}, {0, cols - 1}, {rows - 1, 0}, {rows - 1, cols - 1}};
        for (int[] k : corners) {
            if (grid[k[0]][k[1]] == 0 && !visited[k[0]][k[1]]) {
                visited[k[0]][k[1]] = true;
                queue.add(k);
            }
        }
        if (queue.isEmpty()) {
            List<String> names = new ArrayList<>();
            for (int[] k : corners) {
                String n = label(k[0], k[1]);
                if (!names.contains(n)) names.add(n);
            }
            return List.of("Lối đi phải xuất phát từ góc phòng: đặt ít nhất một trong các góc "
                    + String.join(", ", names) + " là lối đi.");
        }

        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            for (int[] d : DIRECTIONS) {
                int r = cur[0] + d[0];
                int c = cur[1] + d[1];
                if (r < 0 || r >= rows || c < 0 || c >= cols) continue;
                if (visited[r][c] || grid[r][c] != 0) continue;
                visited[r][c] = true;
                queue.add(new int[]{r, c});
            }
        }

        List<String> isolated = new ArrayList<>();
        int total = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (grid[r][c] == 0 && !visited[r][c]) {
                    total++;
                    if (isolated.size() < MAX_LISTED) isolated.add(label(r, c));
                }
            }
        }
        if (total == 0) return List.of();
        return List.of("Lối đi tại " + String.join(", ", isolated) + (total > MAX_LISTED ? "..." : "")
                + " không nối liền với lối đi ở góc phòng. Các ô lối đi phải liền nhau và thông ra ít nhất một góc.");
    }

    /**
     * BR-SM-06  Ghế Sweetbox (ghế đôi) phải đi theo cặp: trong mỗi hàng, mỗi dãy Sweetbox liền nhau
     * phải có số ghế chẵn (ghép từ trái sang phải: 1-2, 3-4...). Ghế hỏng vẫn tính vì vẫn chiếm chỗ.
     */
    static List<String> checkSweetboxPairs(int[][] grid, Set<Integer> sweetboxTypeIds) {
        List<String> errors = new ArrayList<>();
        if (sweetboxTypeIds.isEmpty()) return errors;
        for (int r = 0; r < grid.length; r++) {
            int c = 0;
            while (c < grid[r].length) {
                if (!sweetboxTypeIds.contains(grid[r][c])) {
                    c++;
                    continue;
                }
                int start = c;
                while (c < grid[r].length && sweetboxTypeIds.contains(grid[r][c])) c++;
                int length = c - start;
                if (length % 2 != 0) {
                    String range = length == 1 ? "Ghế " + label(r, start)
                            : "Dãy " + label(r, start) + "-" + label(r, c - 1) + " (" + length + " ghế)";
                    errors.add(range + ": ghế Sweetbox phải đi theo cặp, số ghế liền nhau phải là số chẵn.");
                }
            }
        }
        return errors;
    }

    private static String label(int r, int c) {
        return SeatMatrixService.rowLabel(r) + (c + 1);
    }
}
