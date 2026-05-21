import java.sql.*;

public class ProfileUserProbe {
  public static void main(String[] args) throws Exception {
    String url = "jdbc:postgresql://localhost:5432/wheelgo_db";
    try (Connection c = DriverManager.getConnection(url, "postgres", "123")) {
      String[] emails = {"ensarmustafa@gmail.com", "muhamedjakupi@gmail.com"};
      for (String email : emails) {
        try (PreparedStatement ps = c.prepareStatement(
            "select u.id, u.email, u.is_active, u.tenant_id, t.slug, t.schema_name, t.is_active as tenant_active " +
            "from public.users u join public.tenants t on t.id = u.tenant_id where lower(u.email)=lower(?)")) {
          ps.setString(1, email);
          try (ResultSet rs = ps.executeQuery()) {
            System.out.println("EMAIL=" + email);
            boolean any = false;
            while (rs.next()) {
              any = true;
              String userId = rs.getObject("id").toString();
              String schema = rs.getString("schema_name");
              System.out.println("  userId=" + userId + ", active=" + rs.getBoolean("is_active") + ", tenantSlug=" + rs.getString("slug") + ", tenantActive=" + rs.getBoolean("tenant_active") + ", schema=" + schema);
              try (PreparedStatement ps2 = c.prepareStatement("select id, first_name, last_name, phone from \"" + schema + "\".user_profiles where user_id = ?")) {
                ps2.setObject(1, java.util.UUID.fromString(userId));
                try (ResultSet rs2 = ps2.executeQuery()) {
                  if (rs2.next()) {
                    System.out.println("  profileFound=true, profileId=" + rs2.getObject("id") + ", firstName=" + rs2.getString("first_name") + ", lastName=" + rs2.getString("last_name") + ", phone=" + rs2.getString("phone"));
                  } else {
                    System.out.println("  profileFound=false");
                  }
                }
              }
            }
            if (!any) {
              System.out.println("  userNotFound");
            }
          }
        }
      }
    }
  }
}
