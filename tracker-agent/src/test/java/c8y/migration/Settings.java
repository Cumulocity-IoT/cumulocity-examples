/*
 * Copyright (c) 2012-2020 Cumulocity GmbH
 * Copyright (c) 2021 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA,
 * and/or its subsidiaries and/or its affiliates and/or their licensors.
 *
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided
 * for in your License Agreement with Software AG.
 */

package c8y.migration;

import java.util.List;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Settings {

	@Value("${C8Y.baseURL}")
	private String c8yHost;
	
	@Value("${C8Y.cep.user}")
	private String cepUser;
	
	@Value("${C8Y.cep.password}")
	private String cepPassword;
	
	@Value("${C8Y.devicebootstrap.user}")
	private String bootstrapUser;
	
	@Value("${C8Y.devicebootstrap.password}")
	private String bootstrapPassword;
	
	@Value("${C8Y.devicebootstrap.tenant}")
	private String bootstrapTenant;
	
	@Value("${C8Y.tenants:#{null}}")
	private List<String> tenants;


	public String getCepPassword() {
		return cepPassword;
	}

	public String getCepUser() {
		return cepUser;
	}
	
	public String getC8yHost() {
		return c8yHost;
	}
	
	public String getBootstrapUser() {
		return bootstrapUser;
	}

	public String getBootstrapPassword() {
		return bootstrapPassword;
	}

	public String getBootstrapTenant() {
		return bootstrapTenant;
	}
	
	public List<String> getTenants() {
		return tenants;
	}


	@Override
	public String toString() {
		return "Settings [c8yHost=" + c8yHost + ", cepUser=" + cepUser + ", cepPassword=" + cepPassword
				+ ", bootstrapUser=" + bootstrapUser + ", bootstrapPassword=" + bootstrapPassword + ", bootstrapTenant="
				+ bootstrapTenant + ", tenants=" + tenants + "]";
	}

	@PostConstruct
	public void init() {
		System.out.println(this);
	}
	
}
