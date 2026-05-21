import java.sql.*;
public class TenantUsersProbe {
  public static void main(String[] args) throws Exception {
    try (var c = DriverManager.getConnection("jdbc:postgresql://localhost:5432/wheelgo_db", "postgres", "123")) {
      try (var ps = c.prepareStatement("select u.id, u.email, u.role, u.is_active, t.slug from public.users u join public.tenants t on t.id=u.tenant_id where t.slug='meditenant' order by u.created_at desc" );
           var rs = ps.executeQuery()) {
        while (rs.next()) {
          System.out.println(rs.getObject(1)+" | "+rs.getString(2)+" | "+rs.getString(3)+" | active="+rs.getBoolean(4)+" | tenant="+rs.getString(5));
        }
      }
    }
  }
}
