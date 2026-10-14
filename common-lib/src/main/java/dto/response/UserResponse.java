package dto.response;



import domain.UserRole;
import domain.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private Long id;

    private String firstName;

    private String lastName;

    private String email;

    private String phoneNumber;

    private String profileImage;

    private UserStatus userStatus;

    private UserRole role;

    private Boolean enabled;

    private LocalDateTime createdAt;

    private LocalDateTime lastLogin;

}
