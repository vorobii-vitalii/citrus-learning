package org.citrus.learn.accountbalanceservice.entity;

import java.math.BigDecimal;

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
@Table(name = "user_balance")
public class UserBalance {

	@Id
	@Column("user_id")
	private Long userId;

	/**
	 * Always in USD
	 */
	@Column("user_balance")
	private BigDecimal balance;

}
