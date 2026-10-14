package cinemanagement;

import org.apache.catalina.Context;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;

import java.io.File;

/**
 * FILE CHẠY CHƯƠNG TRÌNH.
 * IntelliJ: mở file này, bấm nút ▶ cạnh hàm main.
 * Sau đó mở http://localhost:8080/
 *
 * Working directory của Run Configuration phải là thư mục gốc project (chỗ có pom.xml).
 * Đổi thông tin DB: VM options -Ddb.user=sa -Ddb.password=... (xem DBContext)
 */
public class Main {

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getProperty("port", "8080"));

        File webapp = new File("src/main/webapp").getAbsoluteFile();
        File classes = new File("target/classes").getAbsoluteFile();
        if (!webapp.isDirectory()) {
            throw new IllegalStateException("Không thấy " + webapp
                    + ". Hãy đặt Working directory là thư mục chứa pom.xml.");
        }

        Tomcat tomcat = new Tomcat();
        tomcat.setBaseDir(new File("target/tomcat").getAbsolutePath());
        tomcat.setPort(port);
        tomcat.getConnector();

        Context ctx = tomcat.addWebapp("", webapp.getAbsolutePath());

        // Cho Tomcat thấy class đã biên dịch như thể nằm trong WEB-INF/classes,
        // nhờ đó @WebServlet được quét tự động.
        WebResourceRoot resources = new StandardRoot(ctx);
        resources.addPreResources(new DirResourceSet(
                resources, "/WEB-INF/classes", classes.getAbsolutePath(), "/"));
        ctx.setResources(resources);

        tomcat.start();
        System.out.println("Server: http://localhost:" + port + "/");
        tomcat.getServer().await();
    }
}
