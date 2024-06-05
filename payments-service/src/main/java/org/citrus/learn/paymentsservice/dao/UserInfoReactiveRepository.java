package org.citrus.learn.paymentsservice.dao;

import org.citrus.learn.paymentsservice.entity.UserInfo;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface UserInfoReactiveRepository extends ReactiveCrudRepository<UserInfo, Long> {
}
