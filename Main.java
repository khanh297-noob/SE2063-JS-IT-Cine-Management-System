package cinemanagement;

import java.io.File;

import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;

/**
 * Khoi dong Tomcat embedded. Chay class nay trong IntelliJ (Working directory = thu muc project).
 */
public class Main {

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getProperty("port",
                System.getenv().getOrDefault("PORT", "8080")));

        File webappDir = new File("src/main/webapp");
        File classesDir = new File("target/classes");
        if (!webappDir.exists()) {
            throw new IllegalStateException("Khong tim thay " + webappDir.getAbsolutePath()
                    + " - hay dat Working directory = thu muc goc cua project.");
        }

        Tomcat tomcat = new Tomcat();
        tomcat.setBaseDir(new File("target/tomcat").getAbsolutePath());
        tomcat.setPort(port);
        tomcat.getConnector();

        StandardContext ctx = (StandardContext) tomcat.addWebapp("", webappDir.getAbsolutePath());
        ctx.setParentClassLoader(Main.class.getClassLoader());

        // Cho Tomcat quet @WebServlet/@WebFilter/@WebListener trong target/classes
        WebResourceRoot resources = new StandardRoot(ctx);
        resources.addPreResources(new DirResourceSet(resources, "/WEB-INF/classes",
                classesDir.getAbsolutePath(), "/"));
        ctx.setResources(resources);

        tomcat.start();
        System.out.println("Cine Management System dang chay tai http://localhost:" + port + "/");
        tomcat.getServer().await();
    }
}
