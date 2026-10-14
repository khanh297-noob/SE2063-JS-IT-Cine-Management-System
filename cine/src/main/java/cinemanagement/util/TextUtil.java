package cinemanagement.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/** Xử lý chuỗi tiếng Việt cho tìm kiếm không phân biệt dấu. */
public final class TextUtil {

    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern SPACES = Pattern.compile("\\s+");

    private TextUtil() {}

    /**
     * Bỏ dấu, đổi đ/Đ thành d, viết thường, gộp khoảng trắng.
     * Vd "Đà  Nẵng" -> "da nang". null -> "".
     */
    public static String fold(String s) {
        if (s == null) return "";
        String noMarks = COMBINING_MARKS.matcher(Normalizer.normalize(s, Normalizer.Form.NFD)).replaceAll("");
        noMarks = noMarks.replace('đ', 'd').replace('Đ', 'D');
        return SPACES.matcher(noMarks.toLowerCase(Locale.ROOT)).replaceAll(" ").trim();
    }

    /** true nếu một trong các chuỗi chứa keyword, không phân biệt dấu và hoa thường. keyword rỗng -> true. */
    public static boolean matchesAny(String keyword, String... texts) {
        String k = fold(keyword);
        if (k.isEmpty()) return true;
        for (String t : texts) {
            if (fold(t).contains(k)) return true;
        }
        return false;
    }
}
