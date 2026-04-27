package com.wheelGo.config;

import com.wheelGo.schema.TenantContext;
import org.springframework.jdbc.datasource.DelegatingDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class TenantAwareDataSource extends DelegatingDataSource {

    public TenantAwareDataSource(DataSource targetDataSource) {
        super(targetDataSource);
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection connection = super.getConnection();
        applySearchPath(connection);
        return connection;
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Connection connection = super.getConnection(username, password);
        applySearchPath(connection);
        return connection;
    }

    private void applySearchPath(Connection connection) throws SQLException {
        String schemaName = TenantContext.getCurrentSchema();
        String targetSchema = isValidSchemaName(schemaName) ? schemaName : "public";

        try (Statement statement = connection.createStatement()) {
            statement.execute("SET search_path TO \"" + targetSchema + "\", public");
        }
    }

    private boolean isValidSchemaName(String schemaName) {
        return schemaName != null && schemaName.matches("^[a-z][a-z0-9_]{0,62}$");
    }
}
