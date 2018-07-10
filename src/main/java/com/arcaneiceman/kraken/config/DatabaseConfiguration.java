package com.arcaneiceman.kraken.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import java.util.Arrays;
import java.util.Objects;

@Configuration
@EnableJpaRepositories("com.arcaneiceman.kraken.repository")
@EnableTransactionManagement
public class DatabaseConfiguration implements ServletContextInitializer {

    @Autowired
    private Environment environment;

    public DatabaseConfiguration() {
    }

    @Override
    public void onStartup(ServletContext servletContext) {
        if (Arrays.stream(environment.getActiveProfiles()).anyMatch(s -> Objects.equals(s, "local")))
            initH2Console(servletContext);
    }

    /**
     * Initializes H2 console.
     */
    private void initH2Console(ServletContext servletContext) {
        try {
            // We don't want to include H2 when we are packaging for the "prod" profile and won't
            // actually need it, so we have to load / invoke things at runtime through reflection.
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            Class<?> servletClass = Class.forName("org.h2.server.web.WebServlet", true, loader);
            Servlet servlet = (Servlet) servletClass.newInstance();

            ServletRegistration.Dynamic h2ConsoleServlet = servletContext.addServlet("H2Console", servlet);
            h2ConsoleServlet.addMapping("/h2-console/*");
            h2ConsoleServlet.setInitParameter("-properties", "src/main/resources/");
            h2ConsoleServlet.setInitParameter("webAllowOthers","true");
            h2ConsoleServlet.setLoadOnStartup(1);

        } catch (ClassNotFoundException | LinkageError e) {
            throw new RuntimeException("Failed to load and initialize org.h2.server.web.WebServlet", e);

        } catch (IllegalAccessException | InstantiationException e) {
            throw new RuntimeException("Failed to instantiate org.h2.server.web.WebServlet", e);
        }
    }


}
