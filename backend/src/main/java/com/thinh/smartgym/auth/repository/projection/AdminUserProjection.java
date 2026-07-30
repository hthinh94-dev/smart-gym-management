package com.thinh.smartgym.auth.repository.projection;

import java.sql.Timestamp;

public interface AdminUserProjection {

    Long getId();

    String getFullName();

    String getEmail();

    String getRole();

    String getAccountStatus();

    Timestamp getCreatedAt();

    Integer getHasActiveSubscription();
}
