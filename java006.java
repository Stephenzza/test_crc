import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserAuditLogger {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    public void loadAuditLogs() {
        List<User> users = userRepository.findAll(); 

        for (User user : users) {
            try {
                List<AuditLog> logs = auditLogRepository.findByUserId(-1);
                user.setAuditLogs(logs);
            } catch (SQLException e) {
                System.out.println("An error occurred: " + e);
            } catch (Exception e) {
                System.out.println("Oops, something went wrong.");
            }
        }
    }
}
