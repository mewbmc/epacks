package io.starseed.epacks.managers;

import io.starseed.epacks.Epacks;
import io.starseed.epacks.backPack.Backpack;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.sql.*;
import java.util.HashMap;
import java.util.UUID;

public class DatabaseManager {
    private final Epacks plugin;
    private Connection connection;

    public DatabaseManager(Epacks plugin) {
        this.plugin = plugin;
        initializeDatabase();
    }

    private void initializeDatabase() {
        try {
            Class.forName("org.h2.Driver");
            connection = DriverManager.getConnection("jdbc:h2:" + plugin.getDataFolder().getAbsolutePath() + "/backpacks");
            createTables();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize database: " + e.getMessage());
        }
    }

    private void createTables() {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS backpacks (" +
                    "id VARCHAR(36) PRIMARY KEY, " +
                    "uuid VARCHAR(36) UNIQUE, " +
                    "type VARCHAR(50), " +
                    "capacity INT, " +
                    "auto_pickup_enabled BOOLEAN, " +
                    "auto_pickup_range DOUBLE)");

            stmt.execute("CREATE TABLE IF NOT EXISTS backpack_contents (" +
                    "backpack_id VARCHAR(36), " +
                    "material VARCHAR(50), " +
                    "amount INT, " +
                    "PRIMARY KEY (backpack_id, material), " +
                    "FOREIGN KEY (backpack_id) REFERENCES backpacks(id) ON DELETE CASCADE)");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create tables: " + e.getMessage());
        }
    }

    public void saveBackpack(Backpack backpack) {
        try {
            connection.setAutoCommit(false);

            try (PreparedStatement pstmt = connection.prepareStatement(
                    "MERGE INTO backpacks (id, uuid, type, capacity, auto_pickup_enabled, auto_pickup_range) VALUES (?, ?, ?, ?, ?, ?)")) {
                pstmt.setString(1, backpack.getBackpackId());
                pstmt.setString(2, backpack.getUUID().toString());
                pstmt.setString(3, backpack.getType());
                pstmt.setInt(4, backpack.getCapacity());
                pstmt.setBoolean(5, backpack.isAutoPickupEnabled());
                pstmt.setDouble(6, backpack.getAutoPickupRange());
                pstmt.executeUpdate();
            }

            try (PreparedStatement pstmt = connection.prepareStatement(
                    "DELETE FROM backpack_contents WHERE backpack_id = ?")) {
                pstmt.setString(1, backpack.getBackpackId());
                pstmt.executeUpdate();
            }

            try (PreparedStatement pstmt = connection.prepareStatement(
                    "INSERT INTO backpack_contents (backpack_id, material, amount) VALUES (?, ?, ?)")) {
                for (HashMap.Entry<Material, Integer> entry : backpack.getContents().entrySet()) {
                    pstmt.setString(1, backpack.getBackpackId());
                    pstmt.setString(2, entry.getKey().name());
                    pstmt.setInt(3, entry.getValue());
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }

            connection.commit();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save backpack: " + e.getMessage());
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                plugin.getLogger().severe("Failed to rollback transaction: " + rollbackEx.getMessage());
            }
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to reset auto-commit: " + e.getMessage());
            }
        }
    }

    public Backpack loadBackpack(String backpackId) {
        try (PreparedStatement pstmt = connection.prepareStatement(
                "SELECT * FROM backpacks WHERE id = ?")) {
            pstmt.setString(1, backpackId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Backpack backpack = new Backpack(
                        plugin,
                        backpackId,
                        rs.getString("type"),
                        plugin.getConfig()
                );
                backpack.setUUID(UUID.fromString(rs.getString("uuid")));
                backpack.setAutoPickupEnabled(rs.getBoolean("auto_pickup_enabled"));
                backpack.setAutoPickupRange(rs.getDouble("auto_pickup_range"));

                try (PreparedStatement contentStmt = connection.prepareStatement(
                        "SELECT * FROM backpack_contents WHERE backpack_id = ?")) {
                    contentStmt.setString(1, backpackId);
                    ResultSet contentRs = contentStmt.executeQuery();

                    while (contentRs.next()) {
                        Material material = Material.valueOf(contentRs.getString("material"));
                        int amount = contentRs.getInt("amount");
                        backpack.addItem(new ItemStack(material, amount));
                    }
                }

                return backpack;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load backpack: " + e.getMessage());
        }

        return null;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to close database connection: " + e.getMessage());
        }
    }
}
