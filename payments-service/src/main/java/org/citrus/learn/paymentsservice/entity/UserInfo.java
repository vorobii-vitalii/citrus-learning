package org.citrus.learn.paymentsservice.entity;

import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "user_info")
public class UserInfo {

	@Id
	@Column("user_id")
	private Long id;

	@Column("is_blocked")
	private boolean isBlocked;

}
